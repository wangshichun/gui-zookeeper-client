package com.github.wangshichun.tools.zookeeper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

/**
 * Created by wangshichun on 2016/5/26.
 */
public class IconUtil {
    static Image getIconImage(String nameAndSuffix) {
        try {
            return ImageIO.read(IconUtil.class.getClassLoader().getResource("icons/" + nameAndSuffix));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
