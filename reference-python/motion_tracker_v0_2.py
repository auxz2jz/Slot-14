#!/usr/bin/env python3
"""Security Camera Motion Tracker v0.2

Changes from v0.1:
- Keeps the original COLOR frame for appearance matching.
- Uses a 2D HSV histogram per moving region to help preserve track identity.
- Predicts the next position from recent velocity.
- Keeps a track alive for short motion dropouts (coasting/predicted state).
- Hybrid mask uses direct frame difference plus nearby adaptive-background pixels.
- Has a short MOG2 warm-up so the first frame is not treated as one huge object.
- Can analyze at reduced resolution while writing an annotated reduced-resolution test video.

The black/white mask is only a debug representation: white = motion/change, black = no
meaningful motion. Tracking itself is NOT limited to black-and-white information.
"""
from __future__ import annotations

import argparse
import math
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import cv2
import numpy as np

PointF = Tuple[float, float]
Point = Tuple[int, int]
Box = Tuple[int, int, int, int]


def box_centroid(b: Box) -> Point:
    x, y, w, h = b
    return (x + w // 2, y + h // 2)


def box_area(b: Box) -> int:
    return max(0, b[2]) * max(0, b[3])


def clamp_box(b: Box, width: int, height: int) -> Box:
    x, y, w, h = b
    x = max(0, min(width - 1, x))
    y = max(0, min(height - 1, y))
    w = max(1, min(width - x, w))
    h = max(1, min(height - y, h))
    return x, y, w, h


def translate_box(b: Box, dx: float, dy: float, width: int, height: int) -> Box:
    x, y, w, h = b
    return clamp_box((int(round(x + dx)), int(round(y + dy)), w, h), width, height)


def dist(a: PointF, b: PointF) -> float:
    return math.hypot(a[0] - b[0], a[1] - b[1])


def boxes_close(a: Box, b: Box, gap: int) -> bool:
    ax, ay, aw, ah = a
    bx, by, bw, bh = b
    hgap = max(0, max(ax, bx) - min(ax + aw, bx + bw))
    vgap = max(0, max(ay, by) - min(ay + ah, by + bh))
    return hgap <= gap and vgap <= gap


def union_box(a: Box, b: Box) -> Box:
    ax, ay, aw, ah = a
    bx, by, bw, bh = b
    l, t = min(ax, bx), min(ay, by)
    r, bot = max(ax + aw, bx + bw), max(ay + ah, by + bh)
    return l, t, r - l, bot - t


def merge_boxes(boxes: List[Box], gap: int) -> List[Box]:
    boxes = boxes[:]
    again = True
    while again:
        again = False
        out: List[Box] = []
        used = [False] * len(boxes)
        for i, b in enumerate(boxes):
            if used[i]: continue
            cur = b; used[i] = True; expanded = True
            while expanded:
                expanded = False
                for j, other in enumerate(boxes):
                    if used[j]: continue
                    if boxes_close(cur, other, gap):
                        cur = union_box(cur, other); used[j] = True; expanded = True; again = True
            out.append(cur)
        boxes = out
    return boxes


def region_hist(frame_bgr: np.ndarray, mask: np.ndarray, box: Box) -> np.ndarray:
    x, y, w, h = box
    roi = frame_bgr[y:y+h, x:x+w]
    roi_mask = mask[y:y+h, x:x+w]
    hsv = cv2.cvtColor(roi, cv2.COLOR_BGR2HSV)
    hist = cv2.calcHist([hsv], [0, 1], roi_mask, [24, 16], [0, 180, 0, 256])
    if hist is None or hist.size == 0 or float(hist.sum()) <= 0:
        hist = cv2.calcHist([hsv], [0, 1], None, [24, 16], [0, 180, 0, 256])
    cv2.normalize(hist, hist, 0.0, 1.0, cv2.NORM_MINMAX)
    return hist


def hist_similarity(a: Optional[np.ndarray], b: Optional[np.ndarray]) -> float:
    if a is None or b is None: return 0.5
    v = float(cv2.compareHist(a, b, cv2.HISTCMP_CORREL))
    return max(0.0, min(1.0, (v + 1.0) * 0.5))


@dataclass
class Detection:
    box: Box
    centroid: Point
    area: int
    hist: Optional[np.ndarray]


@dataclass
class Track:
    track_id: int
    box: Box
    centroid: PointF
    hist: Optional[np.ndarray]
    velocity: PointF = (0.0, 0.0)
    path: List[Point] = field(default_factory=list)
    missed: int = 0
    age: int = 1
    confirmed_hits: int = 1
    predicted: bool = False
    match_quality: float = 1.0

    def predicted_centroid(self) -> PointF:
        horizon = min(3.0, 1.0 + self.missed * 0.35)
        return (self.centroid[0] + self.velocity[0] * horizon,
                self.centroid[1] + self.velocity[1] * horizon)


class TrackManager:
    def __init__(self, max_distance: float, max_missed: int, max_path: int):
        self.max_distance = max_distance
        self.max_missed = max_missed
        self.max_path = max_path
        self.next_id = 1
        self.tracks: Dict[int, Track] = {}

    def update(self, detections: List[Detection], frame_size: Tuple[int, int]) -> Dict[int, Track]:
        height, width = frame_size
        candidates: List[Tuple[float, int, int, float]] = []
        for tid, tr in self.tracks.items():
            pred = tr.predicted_centroid()
            gate = self.max_distance * (1.0 + min(tr.missed, 8) * 0.18)
            old_area = max(1, box_area(tr.box))
            for di, d in enumerate(detections):
                dd = dist(pred, d.centroid)
                if dd > gate: continue
                new_area = max(1, d.area)
                ratio = max(old_area, new_area) / min(old_area, new_area)
                size_cost = min(1.0, max(0.0, (ratio - 1.0) / 3.0))
                app = hist_similarity(tr.hist, d.hist)
                cost = 0.58 * min(1.0, dd / gate) + 0.28 * (1.0 - app) + 0.14 * size_cost
                candidates.append((cost, tid, di, app))
        candidates.sort(key=lambda x: x[0])
        used_t, used_d = set(), set()
        for cost, tid, di, app in candidates:
            if tid in used_t or di in used_d or cost > 0.82: continue
            tr, d = self.tracks[tid], detections[di]
            prev = tr.centroid
            dx, dy = d.centroid[0] - prev[0], d.centroid[1] - prev[1]
            tr.velocity = (0.62 * tr.velocity[0] + 0.38 * dx, 0.62 * tr.velocity[1] + 0.38 * dy)
            tr.centroid = (float(d.centroid[0]), float(d.centroid[1]))
            tr.box = d.box
            if tr.hist is None: tr.hist = d.hist
            elif d.hist is not None:
                tr.hist = cv2.addWeighted(tr.hist, 0.72, d.hist, 0.28, 0.0)
                cv2.normalize(tr.hist, tr.hist, 0.0, 1.0, cv2.NORM_MINMAX)
            tr.missed = 0; tr.age += 1; tr.confirmed_hits += 1; tr.predicted = False
            tr.match_quality = 1.0 - cost
            tr.path.append((int(tr.centroid[0]), int(tr.centroid[1]))); tr.path = tr.path[-self.max_path:]
            used_t.add(tid); used_d.add(di)
        for tid in list(self.tracks):
            if tid in used_t: continue
            tr = self.tracks[tid]; tr.missed += 1; tr.age += 1
            if tr.missed > self.max_missed:
                del self.tracks[tid]; continue
            dx, dy = tr.velocity
            tr.box = translate_box(tr.box, dx, dy, width, height)
            tr.centroid = (tr.centroid[0] + dx, tr.centroid[1] + dy)
            tr.velocity = (dx * 0.82, dy * 0.82); tr.predicted = True; tr.match_quality *= 0.88
            if tr.missed <= 6:
                tr.path.append((int(tr.centroid[0]), int(tr.centroid[1]))); tr.path = tr.path[-self.max_path:]
        for di, d in enumerate(detections):
            if di in used_d: continue
            tid = self.next_id; self.next_id += 1
            self.tracks[tid] = Track(tid, d.box, (float(d.centroid[0]), float(d.centroid[1])), d.hist, path=[d.centroid])
        return self.tracks


def build_motion_mask(gray, previous_gray, bg, threshold, frame_index, warmup_frames):
    cur = cv2.GaussianBlur(gray, (5, 5), 0)
    if previous_gray is None: diff_mask = np.zeros_like(gray)
    else:
        prev = cv2.GaussianBlur(previous_gray, (5, 5), 0)
        delta = cv2.absdiff(cur, prev)
        _, diff_mask = cv2.threshold(delta, threshold, 255, cv2.THRESH_BINARY)
    bg_mask = bg.apply(gray); _, bg_mask = cv2.threshold(bg_mask, 200, 255, cv2.THRESH_BINARY)
    if frame_index <= warmup_frames: combined = diff_mask
    else:
        k9 = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9))
        neighborhood = cv2.dilate(diff_mask, k9, iterations=2)
        nearby_bg = cv2.bitwise_and(bg_mask, neighborhood)
        combined = cv2.bitwise_or(diff_mask, nearby_bg)
    k3 = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3)); k7 = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7))
    cleaned = cv2.morphologyEx(combined, cv2.MORPH_OPEN, k3, iterations=1)
    cleaned = cv2.morphologyEx(cleaned, cv2.MORPH_CLOSE, k7, iterations=2)
    return cv2.dilate(cleaned, k3, iterations=1)


def detect(frame, mask, min_area, merge_gap):
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    boxes = [cv2.boundingRect(c) for c in contours if cv2.contourArea(c) >= min_area]
    out = []
    for b in merge_boxes(boxes, merge_gap):
        if box_area(b) >= min_area: out.append(Detection(b, box_centroid(b), box_area(b), region_hist(frame, mask, b)))
    return out


def draw(frame, mask, tracks):
    out = frame.copy(); motion = mask > 0
    if np.any(motion):
        red = out.copy(); red[motion] = (0, 0, 255); out = cv2.addWeighted(out, 0.66, red, 0.34, 0)
    for tr in tracks.values():
        color = (0, 220, 0) if not tr.predicted else (0, 170, 255)
        x, y, w, h = tr.box; cv2.rectangle(out, (x, y), (x+w, y+h), color, 2)
        if len(tr.path) >= 2:
            pts = np.array(tr.path, dtype=np.int32).reshape((-1, 1, 2)); cv2.polylines(out, [pts], False, color, 2, cv2.LINE_AA)
        state = "TRACK" if not tr.predicted else f"PRED {tr.missed}"
        cv2.putText(out, f"Motion #{tr.track_id} {state} q={tr.match_quality:.2f}", (x, max(18, y-7)), cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 1, cv2.LINE_AA)
    return out


def main():
    p=argparse.ArgumentParser();p.add_argument("input");p.add_argument("--output",default=None);p.add_argument("--analysis-width",type=int,default=640);p.add_argument("--threshold",type=int,default=20);p.add_argument("--min-area",type=int,default=90);p.add_argument("--merge-gap",type=int,default=22);p.add_argument("--max-distance",type=float,default=95.0);p.add_argument("--max-missed",type=int,default=24);p.add_argument("--warmup",type=int,default=12);p.add_argument("--seconds",type=float,default=0.0);args=p.parse_args()
    src=Path(args.input);cap=cv2.VideoCapture(str(src))
    if not cap.isOpened(): raise SystemExit(f"Cannot open {src}")
    fps=cap.get(cv2.CAP_PROP_FPS) or 15.0;sw=int(cap.get(cv2.CAP_PROP_FRAME_WIDTH));sh=int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT));scale=min(1.0,args.analysis_width/float(sw));w,h=int(round(sw*scale)),int(round(sh*scale));max_frames=int(round(args.seconds*fps)) if args.seconds>0 else 0
    out_path=Path(args.output) if args.output else src.with_name(src.stem+"_tracked_v0.2.mp4");mask_path=out_path.with_name(out_path.stem+"_mask.mp4");fourcc=cv2.VideoWriter_fourcc(*"mp4v");writer=cv2.VideoWriter(str(out_path),fourcc,fps,(w,h));mask_writer=cv2.VideoWriter(str(mask_path),fourcc,fps,(w,h),True)
    bg=cv2.createBackgroundSubtractorMOG2(history=400,varThreshold=18.0,detectShadows=True);mgr=TrackManager(args.max_distance,args.max_missed,400);prev_gray=None;idx=0
    while True:
        ok,frame=cap.read()
        if not ok: break
        idx+=1
        if max_frames and idx>max_frames: break
        if scale!=1.0: frame=cv2.resize(frame,(w,h),interpolation=cv2.INTER_AREA)
        gray=cv2.cvtColor(frame,cv2.COLOR_BGR2GRAY);mask=build_motion_mask(gray,prev_gray,bg,args.threshold,idx,args.warmup);tracks=mgr.update(detect(frame,mask,args.min_area,args.merge_gap),(h,w));annotated=draw(frame,mask,tracks);writer.write(annotated);mask_writer.write(cv2.cvtColor(mask,cv2.COLOR_GRAY2BGR));prev_gray=gray
    cap.release();writer.release();mask_writer.release();print(out_path);print(mask_path);return 0

if __name__ == "__main__": raise SystemExit(main())
