import cv2, numpy as np
D='C:/Users/feder/Downloads/'
for name in ['calientabrazos','pantalon','medias']:
    img=cv2.imread(D+name+'.png').astype(int)[:,:,::-1]
    dist=np.sqrt(((img-np.array([201,149,89]))**2).sum(axis=2))
    m=(dist<22).astype(np.uint8)*255
    m=cv2.morphologyEx(m,cv2.MORPH_OPEN,cv2.getStructuringElement(cv2.MORPH_RECT,(9,9)))
    n,lab,st,cen=cv2.connectedComponentsWithStats(m)
    print(name)
    for i in range(1,n):
        x,y,w,h,a=st[i]
        if 60<=w<=140 and 60<=h<=140 and a>0.6*w*h: print('  slot cx=%d cy=%d w=%d h=%d'%(x+w/2,y+h/2,w,h))
