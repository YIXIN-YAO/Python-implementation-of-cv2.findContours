import cv2
import numpy as np


class FindContours:
    def __init__(self, grid):
        self.count = 0
        self.contours = {}
        self.LNBD = 1
        self.NBD = 1
        self.Disp_with_number = True
        self.grid = grid.copy().astype("int32")
        self.grid = np.pad(self.grid, ((1, 1), (1, 1)), 'constant', constant_values=0)  # add edges
        self.MAX_BODER_NUMBER = self.grid.shape[0] * self.grid.shape[1]
        self.contours_dict = {1: self.Contour(-1, "Hole")}

    def Contour(self, parent, contour_type, start_point=[-1, -1]):
        contour = {"parent": parent,
                   "contour_type": contour_type,
                   "son": [],
                   "start_point": start_point}  # Hole/Outer
        return contour

    def find_neighbor(self, center, start, clock_wise=1):
        weight = -1
        if clock_wise == 1:
            weight = 1
        neighbors = np.array([[0, 0], [0, 1], [0, 2], [1, 2], [2, 2], [2, 1], [2, 0], [1, 0]])
        indexs = np.array([[0, 1, 2],
                           [7, 9, 3],
                           [6, 5, 4]])
        start_ind = indexs[start[0] - center[0] + 1][start[1] - center[1] + 1]
        for i in range(1, len(neighbors) + 1):
            cur_ind = (start_ind + i * weight + 8) % 8
            x = neighbors[cur_ind][0] + center[0] - 1
            y = neighbors[cur_ind][1] + center[1] - 1
            if self.grid[x][y] != 0:
                return [x, y]
        return [-1, -1]

    def board_follow(self, center_p, start_p, mode):
        ij = center_p
        ij2 = start_p
        ij1 = self.find_neighbor(ij, ij2, 1)
        x = ij1[0]
        y = ij1[1]
        if ij1 == [-1, -1]:
            self.grid[ij[0]][ij[1]] = -self.NBD
            return
        ij2 = ij1
        ij3 = ij
        self.count += 1
        self.contours[self.count] = []
        for k in range(self.MAX_BODER_NUMBER):
            ij4 = self.find_neighbor(ij3, ij2, 0)
            self.contours[self.count].append([ij4[0]-1, ij4[1]-1])
            x = ij3[0]
            y = ij3[1]
            if ij4[0] - ij2[0] <= 0:
                weight = -1
            else:
                weight = 1
            if self.grid[x][y] < 0:
                self.grid[x][y] = self.grid[x][y]

            elif self.grid[x][y - 1] == 0 and self.grid[x][y + 1] == 0:
                self.grid[x][y] = self.NBD * weight

            elif self.grid[x][y + 1] == 0:
                self.grid[x][y] = -self.NBD

            elif self.grid[x][y] == 1 and self.grid[x][y + 1] != 0:
                self.grid[x][y] = self.NBD

            else:
                self.grid[x][y] = self.grid[x][y]

            if ij4 == ij and ij3 == ij1:
                return
            ij2 = ij3
            ij3 = ij4

    def raster_scan(self):
        for i in range(self.grid.shape[0]):
            self.LNBD = 1
            for j in range(self.grid.shape[1]):
                if abs(self.grid[i][j]) > 1:
                    self.LNBD = abs(self.grid[i][j])
                if self.grid[i][j] >= 1:
                    if self.grid[i][j] == 1 and self.grid[i][j - 1] == 0:
                        self.NBD += 1
                        self.board_follow([i, j], [i, j - 1], 1)
                        border_type = "Outer"

                    elif self.grid[i][j] >= 1 and self.grid[i][j + 1] == 0:
                        border_type = "Hole"
                        self.NBD += 1
                        self.board_follow([i, j], [i, j + 1], 1)
                    else:
                        continue
                    parent = self.LNBD
                    if self.contours_dict[self.LNBD]["contour_type"] == border_type:
                        parent = self.contours_dict[self.LNBD]["parent"]
                    self.contours_dict[self.NBD] = self.Contour(parent, border_type, [i - 1, j - 1])
                    self.contours_dict[parent]["son"].append(self.NBD)
        self.grid = self.grid[1:-1, 1:-1]  # cut edge


def expand_img_point(img, expand):
    new_img = np.ones((img.shape[0] * expand, img.shape[1] * expand, 3)) * 255
    for i in range(img.shape[0]):
        for j in range(img.shape[1]):
            if img[i][j] == 1:
                new_img[i*expand][j*expand][:] = 0
    return new_img


def main():
    img = cv2.imread("map2.png")
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    ret, binary = cv2.threshold(src=gray, thresh=50, maxval=1, type=cv2.THRESH_BINARY)
    fc = FindContours(binary)
    fc.raster_scan()
    print(fc.contours_dict)
    print(fc.contours)
    # set contours green
    for i in fc.contours:
        for point in fc.contours[i]:
            img[point[0], point[1]] = (0, 255, 0)
    cv2.imwrite("./res/res.png", img)
    cv2.imshow("img", img)
    cv2.waitKey(0)
    # get a point map
    expand = 5
    point_map = expand_img_point(binary, expand)
    cv2.imwrite("./res/point_map_origin.png", point_map)
    cv2.imshow("point_map_origin", point_map)
    cv2.waitKey(0)
    # draw contours on point map
    for i in fc.contours:
        for id in range(len(fc.contours[i])):
            cv2.line(point_map, (expand * fc.contours[i][id][1], expand * fc.contours[i][id][0]), (
            expand * fc.contours[i][(id + 1) % len(fc.contours[i])][1],
            expand * fc.contours[i][(id + 1) % len(fc.contours[i])][0]), (0, 0, 255), 1)
    cv2.imwrite("./res/point_map.png", point_map)
    cv2.imshow("point_map", point_map)
    cv2.waitKey(0)

if __name__ == "__main__":
    main()