"""PC방 화면 설계도 SVG 생성기. 사용: python3 gen_design.py <출력 폴더>"""
import os
import sys
from xml.sax.saxutils import escape

OUT = sys.argv[1]
FONT = "Apple SD Gothic Neo, Malgun Gothic, Noto Sans KR, sans-serif"
C = {
    "canvas": "#FFFFFF", "win": "#F5F5F7", "border": "#C7C7CC", "text": "#1D1D1F",
    "sub": "#6E6E73", "field": "#FFFFFF", "titlebar": "#E8E8ED", "primary": "#0A84FF",
    "empty": "#E6E6E6", "using": "#AAD7FF", "selected": "#1E6EDC", "marker": "#FF3B30",
    "header": "#EDEDF0", "rowsel": "#D6E8FF", "note": "#8E8E93",
}
PAD = 32
TITLE_H = 28


class Screen:
    def __init__(self, slug, title, w, h):
        self.slug, self.title, self.w, self.h = slug, title, w, h
        self.parts = []
        self.legend = []
        self.ox, self.oy = PAD, PAD + 40 + TITLE_H   # 콘텐츠 원점 (창 제목줄 아래)

    # ---------- 기본 도형 (콘텐츠 기준 좌표) ----------
    def add(self, s):
        self.parts.append(s)

    def g_open(self, gid):
        self.add(f'<g id="{escape(gid)}">')

    def g_close(self):
        self.add("</g>")

    def rect(self, x, y, w, h, fill="none", stroke=None, sw=1, rx=0, dash=None, absolute=False):
        if not absolute:
            x, y = x + self.ox, y + self.oy
        s = f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}"'
        if stroke:
            s += f' stroke="{stroke}" stroke-width="{sw}"'
        if dash:
            s += f' stroke-dasharray="{dash}"'
        self.add(s + "/>")

    def text(self, x, y, s, size=13, weight=400, anchor="start", fill=None, absolute=False):
        if not absolute:
            x, y = x + self.ox, y + self.oy
        fill = fill or C["text"]
        self.add(f'<text x="{x}" y="{y}" font-family="{FONT}" font-size="{size}" font-weight="{weight}" '
                 f'text-anchor="{anchor}" fill="{fill}">{escape(str(s))}</text>')

    def line(self, x1, y1, x2, y2, stroke=None, sw=1):
        stroke = stroke or C["border"]
        self.add(f'<line x1="{x1 + self.ox}" y1="{y1 + self.oy}" x2="{x2 + self.ox}" y2="{y2 + self.oy}" '
                 f'stroke="{stroke}" stroke-width="{sw}"/>')

    # ---------- Swing 부품 ----------
    def label(self, x, y, s, size=13, weight=400, anchor="start", fill=None):
        self.text(x, y, s, size, weight, anchor, fill)

    def button(self, x, y, w, label, h=26, primary=False, gid=None):
        self.g_open(gid or f"JButton {label}")
        self.rect(x, y, w, h, fill=C["primary"] if primary else C["field"], stroke=None if primary else C["border"], rx=6)
        self.text(x + w / 2, y + h / 2 + 4.5, label, 13, 500, "middle", "#FFFFFF" if primary else C["text"])
        self.g_close()

    def field(self, x, y, w, value="", h=26, password=False, placeholder=False, gid="JTextField"):
        self.g_open(gid)
        self.rect(x, y, w, h, fill=C["field"], stroke=C["border"], rx=4)
        if password and value:
            value = "●" * len(value)
        if value:
            self.text(x + 8, y + h / 2 + 4.5, value, 13, 400, "start", C["sub"] if placeholder else C["text"])
        self.g_close()

    def combo(self, x, y, w, value, h=26, gid="JComboBox"):
        self.g_open(gid)
        self.rect(x, y, w, h, fill=C["field"], stroke=C["border"], rx=4)
        self.text(x + 8, y + h / 2 + 4.5, value, 13)
        self.rect(x + w - 22, y + 4, 18, h - 8, fill=C["primary"], rx=3)
        self.text(x + w - 13, y + h / 2 + 4, "▾", 11, 700, "middle", "#FFFFFF")
        self.g_close()

    def spinner(self, x, y, w, value, h=26):
        self.g_open("JSpinner")
        self.rect(x, y, w, h, fill=C["field"], stroke=C["border"], rx=4)
        self.text(x + 8, y + h / 2 + 4.5, value, 13)
        self.text(x + w - 12, y + 11, "▴", 9, 400, "middle", C["sub"])
        self.text(x + w - 12, y + 21, "▾", 9, 400, "middle", C["sub"])
        self.g_close()

    def titled(self, x, y, w, h, title, gid):
        self.g_open(gid)
        self.rect(x, y + 8, w, h - 8, stroke=C["border"], rx=4)
        tw = len(title) * 12 + 12
        self.rect(x + 10, y, tw, 16, fill=C["win"])
        self.text(x + 16, y + 12, title, 12, 500, "start", C["sub"])
        self.g_close()

    def textarea(self, x, y, w, h, lines, gid="JTextArea (JScrollPane)"):
        self.g_open(gid)
        self.rect(x, y, w, h, fill=C["field"], stroke=C["border"], rx=4)
        for i, s in enumerate(lines):
            self.text(x + 8, y + 20 + i * 20, s, 12.5)
        self.g_close()

    def tabs(self, x, y, names, active, w_total):
        self.g_open("JTabbedPane 탭")
        cx = x
        for n in names:
            tw = len(n) * 13 + 28
            on = n == active
            self.rect(cx, y, tw, 28, fill=C["field"] if on else C["header"], stroke=C["border"], rx=4)
            if on:
                self.rect(cx + 1, y + 24, tw - 2, 6, fill=C["field"])
            self.text(cx + tw / 2, y + 18, n, 13, 600 if on else 400, "middle", C["text"] if on else C["sub"])
            cx += tw + 2
        self.g_close()

    def table(self, x, y, w, cols, rows, selected=None, gid="JTable (JScrollPane)", h=None):
        """cols: [(제목, 너비비율)], rows: [[값...]]"""
        total = sum(c[1] for c in cols)
        widths = [w * c[1] / total for c in cols]
        rh = 26
        h = h or rh * (len(rows) + 1) + 8
        self.g_open(gid)
        self.rect(x, y, w, h, fill=C["field"], stroke=C["border"], rx=4)
        self.rect(x + 1, y + 1, w - 2, rh, fill=C["header"])
        cx = x
        for (name, _), cw in zip(cols, widths):
            self.text(cx + 8, y + 18, name, 12.5, 600)
            cx += cw
        for r, row in enumerate(rows):
            ry = y + rh * (r + 1)
            if r == selected:
                self.rect(x + 1, ry, w - 2, rh, fill=C["rowsel"])
            self.line(x, ry, x + w, ry, C["header"])
            cx = x
            for val, cw in zip(row, widths):
                self.text(cx + 8, ry + 18, val, 12.5)
                cx += cw
        self.g_close()

    def marker(self, x, y, n, desc):
        """번호 표시 + 아래 범례에 설명 추가"""
        self.legend.append((n, desc))
        ax, ay = x + self.ox, y + self.oy
        self.add(f'<g id="표시 {n}"><circle cx="{ax}" cy="{ay}" r="10" fill="{C["marker"]}"/>'
                 f'<text x="{ax}" y="{ay + 4}" font-family="{FONT}" font-size="11" font-weight="700" '
                 f'text-anchor="middle" fill="#FFFFFF">{n}</text></g>')

    # ---------- 출력 ----------
    def render(self):
        win_w, win_h = self.w, self.h
        legend_h = 40 + 22 * len(self.legend) if self.legend else 0
        cw = win_w + PAD * 2
        ch = PAD + 40 + win_h + legend_h + PAD
        out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{cw}" height="{ch}" viewBox="0 0 {cw} {ch}">',
               f'<title>{escape(self.title)}</title>',
               f'<rect width="{cw}" height="{ch}" fill="{C["canvas"]}"/>',
               f'<text x="{PAD}" y="{PAD + 18}" font-family="{FONT}" font-size="18" font-weight="700" fill="{C["text"]}">{escape(self.title)}</text>',
               f'<text x="{PAD + cw - PAD * 2}" y="{PAD + 18}" font-family="{FONT}" font-size="12" text-anchor="end" fill="{C["note"]}">{win_w} × {win_h}</text>']
        wx, wy = PAD, PAD + 40
        out.append('<g id="창">')
        out.append(f'<rect x="{wx}" y="{wy}" width="{win_w}" height="{win_h}" rx="10" fill="{C["win"]}" stroke="{C["border"]}"/>')
        out.append(f'<path d="M{wx} {wy + TITLE_H}V{wy + 10}a10 10 0 0 1 10 -10H{wx + win_w - 10}a10 10 0 0 1 10 10V{wy + TITLE_H}Z" fill="{C["titlebar"]}"/>')
        for i, col in enumerate(["#FF5F57", "#FEBC2E", "#28C840"]):
            out.append(f'<circle cx="{wx + 16 + i * 20}" cy="{wy + 14}" r="6" fill="{col}"/>')
        out.append(f'<text x="{wx + win_w / 2}" y="{wy + 19}" font-family="{FONT}" font-size="13" font-weight="600" text-anchor="middle" fill="{C["text"]}">{escape(self.window_title)}</text>')
        out.append("</g>")
        out.append('<g id="내용">')
        out.extend(self.parts)
        out.append("</g>")
        if self.legend:
            ly = wy + win_h + 32
            out.append('<g id="범례">')
            out.append(f'<text x="{PAD}" y="{ly}" font-family="{FONT}" font-size="13" font-weight="700" fill="{C["text"]}">구성 요소</text>')
            for i, (n, desc) in enumerate(self.legend):
                yy = ly + 24 + i * 22
                out.append(f'<circle cx="{PAD + 9}" cy="{yy - 4}" r="9" fill="{C["marker"]}"/>')
                out.append(f'<text x="{PAD + 9}" y="{yy}" font-family="{FONT}" font-size="10.5" font-weight="700" text-anchor="middle" fill="#FFFFFF">{n}</text>')
                out.append(f'<text x="{PAD + 26}" y="{yy}" font-family="{FONT}" font-size="12.5" fill="{C["text"]}">{escape(desc)}</text>')
            out.append("</g>")
        out.append("</svg>")
        path = os.path.join(OUT, f"{self.slug}.svg")
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(out))
        print(path)


def screen(slug, title, window_title, w, h):
    s = Screen(slug, title, w, h)
    s.window_title = window_title
    return s


# ======================================================================
# 01 관리자 로그인 (AdminLoginFrame 320 × 200)
s = screen("01_관리자_로그인", "01 관리자 로그인 · AdminLoginFrame", "관리자 로그인", 320, 200)
s.label(20, 40, "아이디")
s.field(130, 22, 170, "admin")
s.label(20, 76, "비밀번호")
s.field(130, 58, 170, "admin", password=True)
s.button(20, 120, 280, "로그인", h=30, primary=True)
s.marker(10, 22, 1, "GridLayout(0, 2) 폼: 아이디 / 비밀번호 (비밀번호 칸에서 Enter = 로그인)")
s.marker(10, 120, 2, "로그인: is_admin 계정만 통과 → dispose() 후 AdminFrame 열기")
s.render()

# ======================================================================
# 관리자 창 공통 (AdminFrame 1000 × 650)
ADMIN_W, ADMIN_H = 1000, 650
CH = ADMIN_H - TITLE_H          # 콘텐츠 높이 622
TAB_NAMES = ["좌석", "회원 관리", "메뉴 관리", "주문", "매출"]


def admin_screen(slug, title, active):
    s = screen(slug, title, "PC방 관리자 - 관리자", ADMIN_W, ADMIN_H)
    s.button(8, 6, 104, "게스트 추가", primary=True, gid="JButton 게스트 추가")
    s.tabs(8, 40, TAB_NAMES, active, ADMIN_W - 16)
    s.rect(8, 67, ADMIN_W - 16, CH - 75, fill=C["field"], stroke=C["border"], rx=4)
    return s


# 02 좌석 탭
s = admin_screen("02_관리자_좌석", "02 관리자 · 좌석 탭", "좌석")
board_x, board_y, board_w, board_h = 16, 75, 696, CH - 91
cell_w, cell_h = (board_w - 24) / 4, (board_h - 16) / 3
using = {1: ("홍길동", "00:42:17"), 3: ("테스트", "00:58:03"), 6: ("김철수", "01:30:00")}
s.g_open("좌석판 GridLayout(0, 4)")
for i in range(12):
    no = i + 1
    cx = board_x + (i % 4) * (cell_w + 8)
    cy = board_y + (i // 4) * (cell_h + 8)
    sel = no == 3
    s.g_open(f"좌석 {no}")
    s.rect(cx, cy, cell_w, cell_h, fill=C["using"] if no in using else C["empty"],
           stroke=C["selected"] if sel else "#8E8E93", sw=3 if sel else 1, rx=4)
    mid = cx + cell_w / 2
    if no in using:
        name, t = using[no]
        s.text(mid, cy + cell_h / 2 - 16, f"{no}번", 14, 600, "middle")
        s.text(mid, cy + cell_h / 2 + 4, name, 14, 400, "middle")
        s.text(mid, cy + cell_h / 2 + 24, t, 14, 400, "middle")
    else:
        s.text(mid, cy + cell_h / 2 - 4, f"{no}번", 14, 600, "middle")
        s.text(mid, cy + cell_h / 2 + 16, "빈자리", 14, 400, "middle", C["sub"])
    s.g_close()
s.g_close()

side_x, side_w = 720, 264
s.g_open("오른쪽 패널")
s.label(side_x, 92, "선택된 좌석: 3번", 13, 600)
s.button(side_x, 104, side_w, "시간 +10분")
s.button(side_x, 134, side_w, "시간 -10분")
s.button(side_x, 164, side_w, "강제 종료")
s.titled(side_x, 200, side_w, CH - 216, "채팅 (선택된 좌석에게 전송)", "채팅")
s.textarea(side_x + 10, 220, side_w - 20, CH - 290, [
    "[3번 테스트] 라면 언제 나와요?",
    "[관리자 → 3번] 곧 갖다드릴게요",
    "[주문] 3번 테스트: 라면 2개",
])
s.field(side_x + 10, CH - 62, side_w - 20, "메시지 입력 후 Enter", placeholder=True)
s.g_close()

s.marker(8, 6, 1, "게스트 추가: 누를 때마다 new GuestFrame(this) → 창이 40px씩 비켜서 열림")
s.marker(8, 40, 2, "JTabbedPane: 탭을 고르면 BasePanel.refresh()로 DB를 다시 읽음")
s.marker(16, 75, 3, "좌석판: 회색 = 빈자리, 파랑 = 사용 중(이름 + 남은 시간, 1초마다 갱신), 클릭 = 선택(파란 테두리)")
s.marker(side_x, 104, 4, "시간 ±10분 / 강제 종료: 선택한 좌석의 GuestFrame에 addSeconds() / forceLogout()")
s.marker(side_x, 200, 5, "채팅: 선택한 좌석에게 전송. 게스트 메시지와 주문 알림도 여기 표시")
s.render()

# 03 회원 관리 탭
s = admin_screen("03_관리자_회원관리", "03 관리자 · 회원 관리 탭", "회원 관리")
s.label(24, 98, "아이디 / 이름")
s.field(112, 80, 160, "")
s.button(280, 80, 60, "검색")
s.table(16, 116, ADMIN_W - 32,
        [("아이디", 1), ("이름", 1), ("전화번호", 1.3), ("남은 시간", 1), ("구분", 0.8), ("상태", 0.8)],
        [["admin", "관리자", "", "00:00:00", "관리자", ""],
         ["test", "테스트", "", "00:58:03", "회원", "사용 중"],
         ["hong", "홍길동", "010-1234-5678", "01:30:00", "회원", ""]],
        selected=2, h=CH - 116 - 96)
fy = CH - 86
s.label(24, fy + 18, "이름"); s.field(60, fy, 110, "홍길동")
s.label(184, fy + 18, "전화번호"); s.field(244, fy, 140, "010-1234-5678")
s.label(398, fy + 18, "새 비밀번호"); s.field(474, fy, 110, "")
s.label(598, fy + 18, "남은 시간(분)"); s.field(686, fy, 70, "90")
s.button(24, fy + 36, 64, "수정")
s.button(96, fy + 36, 64, "삭제")
s.label(172, fy + 54, "비밀번호는 비워두면 그대로 유지", 12, 400, "start", C["sub"])
s.marker(16, 80, 1, "검색: 아이디 또는 이름에 포함된 회원 (MemberDAO.search, 빈칸 = 전체)")
s.marker(16, 116, 2, "회원 표: 줄을 클릭하면 아래 입력칸이 채워짐. 사용 중인 회원은 '사용 중' 표시")
s.marker(16, fy, 3, "수정: 관리자·사용 중 회원은 수정/삭제 불가, 남은 시간은 분 단위")
s.render()

# 04 메뉴 관리 탭
s = admin_screen("04_관리자_메뉴관리", "04 관리자 · 메뉴 관리 탭", "메뉴 관리")
s.table(16, 80, ADMIN_W - 32,
        [("번호", 0.6), ("이름", 1.4), ("가격", 1), ("분류", 1), ("재고", 0.8)],
        [["5", "과자", "1,500", "간식", "80"],
         ["1", "라면", "3,000", "식사", "48"],
         ["2", "볶음밥", "5,000", "식사", "30"],
         ["4", "아메리카노", "2,000", "음료", "100"],
         ["3", "콜라", "1,500", "음료", "100"]],
        selected=1, h=CH - 80 - 96)
fy = CH - 86
s.label(24, fy + 18, "이름"); s.field(60, fy, 120, "라면")
s.label(194, fy + 18, "가격"); s.field(230, fy, 80, "3000")
s.label(324, fy + 18, "분류"); s.field(360, fy, 80, "식사")
s.label(454, fy + 18, "재고"); s.field(490, fy, 60, "48")
s.button(24, fy + 36, 64, "추가")
s.button(96, fy + 36, 64, "수정")
s.button(168, fy + 36, 64, "삭제")
s.button(240, fy + 36, 84, "새로 입력")
s.marker(16, 80, 1, "메뉴 표: 분류 → 이름 순. 셀은 직접 수정 불가(readOnlyModel)")
s.marker(16, fy, 2, "입력칸: 줄을 클릭하면 채워짐. 가격/재고는 0 이상의 숫자만")
s.marker(16, fy + 36, 3, "추가 / 수정 / 삭제(확인창) / 새로 입력(선택 해제 + 칸 비우기)")
s.render()

# 05 주문 탭
s = admin_screen("05_관리자_주문", "05 관리자 · 주문 탭", "주문")
s.table(16, 80, ADMIN_W - 32,
        [("주문번호", 0.8), ("시간", 1.1), ("좌석", 0.6), ("회원", 0.8), ("메뉴", 1.2), ("수량", 0.6), ("금액", 0.8), ("상태", 0.6)],
        [["2", "11-02 13:42", "3번", "test", "콜라", "1", "1,500", "대기"],
         ["1", "11-02 13:40", "3번", "test", "라면", "2", "6,000", "완료"]],
        selected=0, h=CH - 80 - 52)
s.button(24, CH - 42, 84, "완료 처리")
s.button(116, CH - 42, 84, "주문 취소")
s.button(208, CH - 42, 84, "새로고침")
s.marker(16, 80, 1, "주문 표: 대기 주문이 위, 그 안에서는 최근 주문이 위 (orders + menu JOIN)")
s.marker(16, CH - 42, 2, "완료 처리 = 상태 DONE / 주문 취소 = 삭제 + 재고 복구 + 환불(음수 결제) 기록")
s.render()

# 06 매출 탭
s = admin_screen("06_관리자_매출", "06 관리자 · 매출 탭", "매출")
s.label(24, 98, "오늘 매출", 13, 600)
s.button(96, 80, 84, "새로고침")
for i, (k, v) in enumerate([("시간 판매", "500원"), ("음식 판매", "7,500원"), ("합계", "8,000원")]):
    y = 140 + i * 40
    s.label(24, y, k)
    s.label(160, y, v, 18 if k == "합계" else 13, 700 if k == "합계" else 400)
s.marker(16, 120, 1, "오늘 날짜의 payment 합계 (PaymentDAO.sumToday). 환불은 음수라 자동으로 빠짐")
s.render()

# ======================================================================
# 게스트 창 (GuestFrame 420 × 520)
GW, GH = 420, 520
GCH = GH - TITLE_H

# 07 게스트 로그인
s = screen("07_게스트_로그인", "07 게스트 · 로그인 카드", "PC방 - 게스트", GW, GH)
s.label(GW / 2, 110, "PC방 로그인", 22, 700, "middle")
for i, (k, v, pw) in enumerate([("아이디", "test", False), ("비밀번호", "1234", True)]):
    y = 160 + i * 44
    s.label(48, y + 18, k)
    s.field(150, y, 222, v, h=30, password=pw)
s.label(48, 160 + 88 + 18, "좌석")
s.combo(150, 160 + 88, 222, "3", h=30)
s.button(GW / 2 - 104, 340, 100, "로그인", h=30, primary=True)
s.button(GW / 2 + 4, 340, 100, "회원가입", h=30)
s.marker(38, 160, 1, "아이디 / 비밀번호 (비밀번호 칸에서 Enter = 로그인)")
s.marker(38, 248, 2, "좌석: 빈 좌석만 표시 (SeatDAO.findEmptySeatNos)")
s.marker(GW / 2 - 114, 340, 3, "로그인: 중복 접속 · 관리자 계정 · 이미 찬 좌석 검사 후 사용 중 카드로 전환")
s.marker(GW / 2 + 104, 340, 4, "회원가입: SignUpDialog (모달)")
s.render()

# 08 게스트 사용 중
s = screen("08_게스트_사용중", "08 게스트 · 사용 중 카드", "PC방 - 게스트", GW, GH)
s.label(GW / 2, 34, "3번 좌석 · 테스트님", 13, 400, "middle")
s.label(GW / 2, 64, "남은 시간", 13, 400, "middle", C["sub"])
s.label(GW / 2, 108, "00:58:03", 36, 700, "middle")
s.titled(12, 128, GW - 24, GCH - 128 - 52, "관리자 채팅", "채팅")
s.textarea(22, 148, GW - 44, GCH - 128 - 52 - 74, [
    "나: 라면 언제 나와요?",
    "관리자: 곧 갖다드릴게요",
    "[주문] 라면 2개 (6,000원)",
])
s.field(22, GCH - 52 - 40, GW - 44, "메시지 입력 후 Enter", placeholder=True)
bx = GW / 2 - 150
s.button(bx, GCH - 40, 96, "시간 구매")
s.button(bx + 102, GCH - 40, 96, "음식 주문")
s.button(bx + 204, GCH - 40, 96, "사용 종료")
s.marker(70, 96, 1, "남은 시간: TimerThread가 1초마다 감소, 5분 남으면 경고, 0이면 자동 종료")
s.marker(12, 128, 2, "관리자 채팅: Enter로 전송 (admin.receiveMessage)")
s.marker(bx - 10, GCH - 40, 3, "시간 구매 / 음식 주문 → 팝업, 사용 종료 → 시간 저장 후 로그인 카드로")
s.render()

# 09 회원가입
s = screen("09_회원가입", "09 회원가입 · SignUpDialog (모달)", "회원가입", 300, 236)
for i, (k, v, pw) in enumerate([("아이디", "hong", False), ("비밀번호", "1234", True), ("이름", "홍길동", False), ("전화번호", "010-1234-5678", False)]):
    y = 16 + i * 34
    s.label(16, y + 18, k)
    s.field(110, y, 174, v, password=pw)
s.button(16, 160, 268, "가입", h=30, primary=True)
s.marker(8, 16, 1, "아이디 · 비밀번호 · 이름은 필수, 전화번호는 선택")
s.marker(8, 160, 2, "가입: 아이디 중복 확인(existsById) 후 MemberDAO.insert")
s.render()

# 10 시간 구매 팝업
s = screen("10_팝업_시간구매", "10 팝업 · 시간 구매 (JOptionPane.showInputDialog)", "시간 구매", 340, 170)
s.add(f'<circle cx="{s.ox + 40}" cy="{s.oy + 44}" r="20" fill="{C["primary"]}"/>')
s.text(40, 51, "?", 22, 700, "middle", "#FFFFFF")
s.label(76, 32, "구매할 시간을 선택하세요.")
s.combo(76, 44, 244, "30분 - 500원")
s.button(158, 98, 76, "취소")
s.button(244, 98, 76, "확인", primary=True)
s.marker(66, 44, 1, "요금표 (time_product): 30분 500원 / 1시간 1,000원 / 3시간 3,000원 / 5시간 5,000원")
s.marker(148, 98, 2, "확인: payment 기록(TIME) → 시간 추가 → DB 저장")
s.render()

# 11 음식 주문 팝업
s = screen("11_팝업_음식주문", "11 팝업 · 음식 주문 (JOptionPane.showConfirmDialog)", "음식 주문", 320, 170)
s.label(20, 36, "메뉴"); s.combo(80, 18, 220, "라면 - 3,000원")
s.label(20, 70, "수량"); s.spinner(80, 52, 220, "2")
s.button(138, 98, 76, "취소")
s.button(224, 98, 76, "확인", primary=True)
s.marker(70, 18, 1, "메뉴: menu 테이블 전체")
s.marker(70, 52, 2, "수량: 1 ~ 10 (SpinnerNumberModel)")
s.marker(128, 98, 3, "확인: 재고 차감(부족하면 거절) → 주문 · 결제 저장 → 관리자에게 알림")
s.render()


# ======================================================================
# 00 화면 흐름도
W, H = 1000, 720
parts = []


def box(x, y, w, h, title, sub=None, accent=False, gid=None):
    parts.append(f'<g id="{escape(gid or title)}">')
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="{"#E5F1FF" if accent else "#FFFFFF"}" '
                 f'stroke="{C["primary"] if accent else "#8E8E93"}" stroke-width="{2 if accent else 1.25}"/>')
    ty = y + (h / 2 + 5 if not sub else 24)
    parts.append(f'<text x="{x + w / 2}" y="{ty}" font-family="{FONT}" font-size="14" font-weight="700" text-anchor="middle" fill="{C["text"]}">{escape(title)}</text>')
    for i, line in enumerate(sub or []):
        parts.append(f'<text x="{x + w / 2}" y="{y + 44 + i * 17}" font-family="{FONT}" font-size="12" text-anchor="middle" fill="{C["sub"]}">{escape(line)}</text>')
    parts.append("</g>")


def arrow(d, label=None, lx=0, ly=0, anchor="middle", dashed=False, both=False):
    dash = ' stroke-dasharray="5 4"' if dashed else ""
    start = ' marker-start="url(#arrow)"' if both else ""
    parts.append(f'<path d="{d}" fill="none" stroke="#636366" stroke-width="1.25"{dash} marker-end="url(#arrow)"{start}/>')
    if label:
        for i, line in enumerate(label.split("\n")):
            parts.append(f'<text x="{lx}" y="{ly + i * 16}" font-family="{FONT}" font-size="12" text-anchor="{anchor}" fill="{C["sub"]}">{escape(line)}</text>')


parts.append(f'<rect width="{W}" height="{H}" fill="#FFFFFF"/>')
parts.append('<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">'
             '<path d="M0 0L10 5L0 10z" fill="#636366"/></marker></defs>')
parts.append(f'<text x="32" y="44" font-family="{FONT}" font-size="20" font-weight="700" fill="{C["text"]}">00 화면 흐름도</text>')
parts.append(f'<text x="32" y="66" font-family="{FONT}" font-size="13" fill="{C["sub"]}">실선 = 화면 이동, 점선 = 창끼리 메서드 호출 (한 JVM 안)</text>')

box(32, 100, 120, 56, "실행", ["Main"])
box(200, 100, 170, 56, "관리자 로그인", ["01 AdminLoginFrame"])
box(430, 100, 210, 56, "관리자 창 · 좌석", ["02 AdminFrame"], accent=True)
parts.append('<g id="관리 탭">')
parts.append(f'<rect x="740" y="84" width="220" height="200" rx="8" fill="#F5F5F7" stroke="#C7C7CC"/>')
parts.append(f'<text x="756" y="106" font-family="{FONT}" font-size="12" font-weight="700" fill="{C["sub"]}">관리 탭 (BasePanel)</text>')
for i, (n, t) in enumerate([("03", "회원 관리"), ("04", "메뉴 관리"), ("05", "주문"), ("06", "매출")]):
    box(756, 118 + i * 40, 188, 32, f"{n} {t}", gid=f"탭 {t}")
parts.append("</g>")

box(430, 290, 210, 56, "게스트 · 로그인", ["07 GuestFrame"])
box(160, 290, 180, 56, "회원가입", ["09 SignUpDialog"])
box(430, 470, 210, 56, "게스트 · 사용 중", ["08 GuestFrame"], accent=True)
box(340, 610, 170, 48, "시간 구매 팝업", ["10"])
box(560, 610, 170, 48, "음식 주문 팝업", ["11"])
box(32, 450, 250, 96, "종료 (endSession)", ["사용 종료 버튼 · 창 X", "남은 시간 0초 · 관리자 강제 종료"])

arrow("M152 128H198")
arrow("M370 128H428", "admin 계정", 399, 90)
arrow("M640 120H738", "탭 선택", 689, 112)
arrow("M535 156V288", "게스트 추가\n(new GuestFrame)", 545, 214, "start")
arrow("M428 318H342", None, both=True)
arrow("M535 346V468", "로그인 성공\nTimerThread 시작", 545, 400, "start")
arrow("M480 526V608")
arrow("M590 526V608")
arrow("M428 498H284")
arrow("M157 450V400H470V348", "시간 저장 · 좌석 비움", 170, 392, "start")
arrow("M640 498H690V140H642", None, dashed=True)
parts.append(f'<text x="700" y="330" font-family="{FONT}" font-size="12" fill="{C["sub"]}">1초마다 좌석판 갱신</text>')
parts.append(f'<text x="700" y="346" font-family="{FONT}" font-size="12" fill="{C["sub"]}">채팅 · 주문 알림 ↑</text>')
parts.append(f'<text x="700" y="372" font-family="{FONT}" font-size="12" fill="{C["sub"]}">시간 ± · 강제 종료</text>')
parts.append(f'<text x="700" y="388" font-family="{FONT}" font-size="12" fill="{C["sub"]}">채팅 ↓</text>')
parts.append(f'<text x="385" y="308" font-family="{FONT}" font-size="12" text-anchor="middle" fill="{C["sub"]}">가입 / 닫힘</text>')

svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
       f'<title>00 화면 흐름도</title>' + "\n".join(parts) + "</svg>")
path = os.path.join(OUT, "00_화면_흐름도.svg")
with open(path, "w", encoding="utf-8") as f:
    f.write(svg)
print(path)
