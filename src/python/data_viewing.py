import numpy as np
import matplotlib.pyplot as plt


dir_loc = '/Users/Matt/'

est = np.load(dir_loc + 'est_st.npy')
tr = np.load(dir_loc + 'tr_st.npy')
# tr_unsc = np.load(dir_loc + 'tr_st_unsc.npy')
# est_unsc = np.load(dir_loc + 'est_st_unsc.npy')


p = plt.plot(0.04*np.arange(len(est[1:6200,0])), est[1:6200,2],'b',0.04*np.arange(len(tr[1:6200,0])), tr[1:6200,2], 'r', linewidth=0.5)
plt.show()