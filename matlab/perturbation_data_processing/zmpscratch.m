% quick zmp algebra
n = [0, 1, 0];

syms m g xg yg xgdd ygdd z hdot real

pg = [0 yg 0]
Mp = cross(pg, m * [0 -g 0]) - cross(pg, m * [xgdd, ygdd, 0]) - [0 0 hdot];
Fg = [0 -m * g 0] - m * [xgdd, ygdd, 0];
cross(n, Mp) / dot(n, Fg)