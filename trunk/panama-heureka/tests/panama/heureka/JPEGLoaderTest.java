/*
 *  Copyright 2004-2010 Robert Brandner (robert.brandner@gmail.com) 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */
package panama.heureka;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import javax.swing.*;

/**
 * @author ridcully
 *
 */
public class JPEGLoaderTest {

    public static void main(String[] args) throws Exception {
        BufferedImage origImg = ImageIO.read(new File("C:\\Users\\ridcully\\Desktop\\re-export.jpg"));

        JOptionPane.showMessageDialog(null,new JLabel(new ImageIcon(origImg)));

        File newFile = new File("new.png");
        ImageIO.write(origImg, "png", newFile);
        BufferedImage newImg = ImageIO.read(newFile);

        JOptionPane.showMessageDialog(null,new JLabel(
            "New",
            new ImageIcon(newImg),
            SwingConstants.LEFT));
    }
}
