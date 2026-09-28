import cv2, numpy as np
D='C:/Users/feder/Downloads/'
for name in ['calientabrazos','pantalon','medias']:
    img=cv2.imread(D+name+'.png')
    hsv=cv2.cvtColor(img,cv2.COLOR_BGR2HSV)
    pink=cv2.inRange(hsv,(150,100,130),(180,255,255))
    pink=cv2.morphologyEx(pink,cv2.MORPH_CLOSE,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(9,9)))
    big=cv2.morphologyEx(pink,cv2.MORPH_OPEN,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(13,13)))
    n,lab,st,cen=cv2.connectedComponentsWithStats(big)
    print(name)
    for i in range(1,n):
        x,y,w,h,a=st[i]
        if w>=30 and h>=30: print('  pin',x,y,w,h,a)
