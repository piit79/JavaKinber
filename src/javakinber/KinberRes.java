package javakinber;

import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;

public class KinberRes
{
  public static ImageIcon iconBlueKinber;
  public static ImageIcon iconRedKinber;
  public static Image imageBlueKinber;

  public static void init(Object obj)
  {
    iconBlueKinber = createImageIcon(obj, "images/JavaKinberBlue.png", "");
    iconRedKinber = createImageIcon(obj, "images/JavaKinberRed.png", "");

    imageBlueKinber = iconBlueKinber.getImage();
  }

  public static ImageIcon createImageIcon(Object o, String path, String description) {
    URL imgURL = o.getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL, description);
    }
    System.err.println("Couldn't find file: " + path);
    return null;
  }
}