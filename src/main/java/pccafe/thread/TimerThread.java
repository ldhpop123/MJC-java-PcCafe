package pccafe.thread;

import pccafe.ui.GuestFrame;

import javax.swing.SwingUtilities;

// 게스트 1명당 1개. 1초마다 남은 시간을 줄이고, 1분마다 DB에 저장한다.
public class TimerThread extends Thread {
    private static final int SAVE_INTERVAL = 60;

    private final GuestFrame guest;

    public TimerThread(GuestFrame guest) {
        this.guest = guest;
        setDaemon(true);
    }

    @Override
    public void run() {
        int tick = 0;
        try {
            while (!isInterrupted() && guest.getRemainSeconds() > 0) {
                Thread.sleep(1000);
                int remain = guest.decreaseOneSecond();
                guest.onTick(remain);
                if (++tick % SAVE_INTERVAL == 0) {
                    guest.saveRemainTime();
                }
            }
            if (!isInterrupted()) {
                SwingUtilities.invokeLater(() -> guest.forceLogout("남은 시간이 모두 소진되었습니다."));
            }
        } catch (InterruptedException e) {
            // 사용 종료로 interrupt() 됨: 정상 종료
        }
    }
}
