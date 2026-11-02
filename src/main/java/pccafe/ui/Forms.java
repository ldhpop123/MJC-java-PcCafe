package pccafe.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class Forms {
    private Forms() {
    }

    // "라벨", 입력칸, "라벨", 입력칸 ... 순서로 받아 한 줄씩 쌓은 폼.
    // GridLayout과 달리 입력칸이 세로로 늘어나지 않고, 라벨 칸은 글자 폭만큼만 차지한다.
    public static JPanel labeled(Object... labelAndFields) {
        JPanel form = new JPanel(new GridBagLayout());
        for (int i = 0; i < labelAndFields.length; i += 2) {
            GridBagConstraints label = new GridBagConstraints();
            label.gridx = 0;
            label.gridy = i / 2;
            label.anchor = GridBagConstraints.WEST;
            label.insets = new Insets(4, 0, 4, 12);
            form.add(new JLabel((String) labelAndFields[i]), label);

            GridBagConstraints field = new GridBagConstraints();
            field.gridx = 1;
            field.gridy = i / 2;
            field.weightx = 1;
            field.fill = GridBagConstraints.HORIZONTAL;
            field.insets = new Insets(4, 0, 4, 0);
            JComponent input = (JComponent) labelAndFields[i + 1];
            if (input instanceof JTextField) {
                field.ipady = 8;   // 기본 입력칸이 얇아서 콤보박스 높이에 맞춤
            }
            form.add(input, field);
        }
        return form;
    }
}
