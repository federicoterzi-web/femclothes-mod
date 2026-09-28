import cv2, numpy as np
img=cv2.imread('C:/Users/feder/Downloads/ChatGPT Image 24 sept 2026, 12_50_36.png').astype(int)[:,:,::-1]
c=img[155,454]; print(c)
dist=np.sqrt(((img-c)**2).sum(axis=2))
m=(dist<40).astype(np.uint8)*255
m=cv2.morphologyEx(m,cv2.MORPH_OPEN,cv2.getStructuringElement(cv2.MORPH_RECT,(21,21)))
n,lab,st,cen=cv2.connectedComponentsWithStats(m)
for i in range(1,n):
    x,y,w,h,a=st[i]
    if 60<=w<=150 and 60<=h<=150: print('slot cx=%d cy=%d w=%d h=%d'%(x+w/2,y+h/2,w,h))
