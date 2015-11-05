# smartcrop-java

smartcrop implementation in Java

smartcrop finds good crops for arbitrary images and crop sizes, based on Jonas Wagner's [smartcrop.js](https://github.com/jwagner/smartcrop.js)


## Sample debug image

> [check more debug image](https://github.com/QuadFlask/smartcrop-java/tree/master/src/test/resources/debug)

![kitty](https://github.com/QuadFlask/smartcrop-java/blob/master/src/test/resources/debug/kitty.png?raw=true)

## Performance 

test code will analyze 12 images, analyze took 30ms 640x470.
> ```
done: 29386679.jpg / analyze took 1158ms
done: 32872321.jpg / analyze took 101ms
done: 65131509.jpg / analyze took 75ms
done: 65158073.jpg / analyze took 109ms
done: 65309527.jpg / analyze took 73ms
done: 65334383.jpg / analyze took 61ms
done: 65356729.jpg / analyze took 55ms
done: 65438769.jpg / analyze took 72ms
done: goodtimes.jpg / analyze took 31ms
done: guitarist.jpg / analyze took 31ms
done: img.jpg / analyze took 30ms
done: kitty.jpg / analyze took 30ms
saved... 29386679.jpg / took 1738ms
saved... 32872321.jpg / took 1475ms
saved... 65131509.jpg / took 1201ms
saved... 65158073.jpg / took 2170ms
saved... 65309527.jpg / took 2397ms
saved... 65334383.jpg / took 1063ms
saved... 65356729.jpg / took 1396ms
saved... 65438769.jpg / took 1359ms
saved... goodtimes.jpg / took 1264ms
saved... guitarist.jpg / took 1190ms
saved... img.jpg / took 702ms
saved... kitty.jpg / took 473ms
```

## Example

> [check test code](https://github.com/QuadFlask/smartcrop-java/blob/master/src/test/java/com/github/quadflask/smartcrop/SmartCropTest.java)

```java
BufferedImage img = ImageIO.read(new File("source.jpg"));

CropResult result = new SmartCrop().analyze(img);

ImageIO.write(result.getBufferedImage(), "png", new File("debug.png"));
```


## Todo

 - [ ] crop feature
 - [ ] analyze after scale down for performance
 - [ ] face detection
 - [ ] remove alpha channel for jpg format
