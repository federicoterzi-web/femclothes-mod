import cv2, numpy as np
D='C:/Users/feder/Downloads/'
img=cv2.imread(D+'calientabrazos.png')
hsv=cv2.cvtColor(img,cv2.COLOR_BGR2HSV)
for (x,y) in [(370,132),(377,120),(520,200),(600,225),(325,180)]:
    print((x,y), img[y,x][::-1], hsv[y,x])
