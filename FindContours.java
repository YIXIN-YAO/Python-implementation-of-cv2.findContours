import java.util.*;

class FindContours {
    private int count = 0;
    public Map<Integer, ArrayList<int[]>> contours = new HashMap<>(); // 存放每一个轮廓
    private int LNDB = 1;
    private int NBD = 1;
    private int[][] grid;
    private int MAX_BODER_NUMBER = 0;
    public Map<Integer, Map<String, Object>> contours_dict = new HashMap<>();

    // 构造函数
    public FindContours(ArrayList<ArrayList<Integer>> agrid) {
        this.grid = pad_grid(agrid); // 加边
        this.MAX_BODER_NUMBER = this.grid.length * this.grid[0].length;
        this.contours_dict.put(1, this.Contour(-1, "Hole", new int[0]));
    }

    public Map<String, Object> Contour(int parent, String contour_type, int[] start_point) {
        if (start_point.length == 0) start_point = new int[]{-1, -1};
        return new HashMap<String, Object>(Map.of("parent", parent, "contour_type", contour_type, "son", new ArrayList<Integer>(), "start_point", start_point));
    }

    public static int[][] pad_grid(ArrayList<ArrayList<Integer>> agrid) {
        agrid.add(0, new ArrayList<Integer>(Collections.nCopies(agrid.get(0).size(),0)));
        agrid.add(new ArrayList<Integer>(Collections.nCopies(agrid.get(0).size(),0)));

        for (ArrayList<Integer> i : agrid) {
            i.add(0);
            i.add(0, 0);
        }
        //ArrayList<ArrayList<Integer>>转int[][]
        int[][] res = agrid.stream().map(i -> i.stream().mapToInt(j -> j).toArray()).toArray(int[][]::new);
        return res;
    }

    public int[] find_neighbor(int[] center, int[] start, boolean clock_wise) {
        int weight = -1;
        if (clock_wise) weight = 1;
        int[][] neighbors = new int[][]{{0, 0}, {0, 1}, {0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}, {1, 0}};
        int[][] indexs = new int[][]{{0, 1, 2},
                {7, 9, 3},
                {6, 5, 4}};
        int start_ind = indexs[start[0] - center[0] + 1][start[1] - center[1] + 1];
        for (int i = 1; i < neighbors.length + 1; i++) {
            int cur_ind = (start_ind + i * weight + 8) % 8;
            int x = neighbors[cur_ind][0] + center[0] - 1;
            int y = neighbors[cur_ind][1] + center[1] - 1;
            if (this.grid[x][y] != 0)
                return new int[]{x, y};
        }
        return new int[]{-1, -1};
    }

    public void board_follow(int[] center_p, int[] start_p) {
        int[] ij = center_p;
        int[] ij2 = start_p;
        int[] ij1 = this.find_neighbor(ij, ij2, true);
        if (Arrays.equals(ij1, new int[]{-1, -1})) {
            this.grid[ij[0]][ij[1]] = -this.NBD;
            return;
        }
        ij2 = ij1;
        int[] ij3 = ij;
        this.count += 1;
        this.contours.put(this.count, new ArrayList<>());
        for (int k = 0; k < this.MAX_BODER_NUMBER; k++) {
            int[] ij4 = this.find_neighbor(ij3, ij2, false);
            this.contours.get(this.count).add(new int[]{ij4[0] - 1, ij4[1] - 1});
            int x = ij3[0];
            int y = ij3[1];
            int weight = 0;
            if ((ij4[0] - ij2[0]) <= 0) weight = -1;
            else weight = 1;
            if (this.grid[x][y] < 0)
                this.grid[x][y] = this.grid[x][y];

            else if (this.grid[x][y - 1] == 0 && this.grid[x][y + 1] == 0)
                this.grid[x][y] = this.NBD * weight;

            else if (this.grid[x][y + 1] == 0)
                this.grid[x][y] = -this.NBD;

            else if (this.grid[x][y] == 1 && this.grid[x][y + 1] != 0)
                this.grid[x][y] = this.NBD;

            else
                this.grid[x][y] = this.grid[x][y];

            if (Arrays.equals(ij4, ij) && Arrays.equals(ij3, ij1))
                return;

            ij2 = ij3;
            ij3 = ij4;
        }
    }

    public void raster_scan() {
        for (int i = 0; i < this.grid.length; i++) {
            this.LNDB = 1;
            for (int j = 0; j < this.grid[0].length; j++) {
                if (Math.abs(this.grid[i][j]) > 1)
                    this.LNDB = Math.abs(this.grid[i][j]);
                if (this.grid[i][j] >= 1) {
                    String border_type;
                    if (this.grid[i][j] == 1 && this.grid[i][j - 1] == 0) {
                        this.NBD += 1;
                        this.board_follow(new int[]{i, j}, new int[]{i, j - 1});
                        border_type = "Outer";
                    } else if (this.grid[i][j] >= 1 && this.grid[i][j + 1] == 0) {
                        border_type = "Hole";
                        this.NBD += 1;
                        this.board_follow(new int[]{i, j}, new int[]{i, j + 1});
                    } else
                        continue;
                    int parent = this.LNDB;
                    if (this.contours_dict.get(this.LNDB).get("contour_type").equals(border_type))
                        parent = (int) this.contours_dict.get(this.LNDB).get("parent");
                    this.contours_dict.put(this.NBD, this.Contour(parent, border_type, new int[]{i - 1, j - 1}));
                    ((ArrayList<Integer>) this.contours_dict.get(parent).get("son")).add(this.NBD);
                }

            }
        }
        // TODO: cut edges

    }

    public static ArrayList<ArrayList<Integer>> readMap(String path) {
        ArrayList<ArrayList<Integer>> a = new ArrayList<>();
        try {
            java.io.File file = new java.io.File(path);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                ArrayList<Integer> b = new ArrayList<>();
                for (int i = 0; i < line.length(); i++) {
                    int temp = line.charAt(i) - '0';
                    if (temp == 0 || temp == 1)
                        b.add(temp);
                }
                a.add(b);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a;
    }

    public static void main(String[] args) {
        ArrayList<ArrayList<Integer>> aa = FindContours.readMap("map0.txt");
//        ArrayList<ArrayList<Integer>> aa = FindContours.readMap("map1.txt");
        FindContours map = new FindContours(aa);
        map.raster_scan();
        // 每一个轮廓的顺序就存储在map.contours里
        System.out.println("contours num: " + map.contours.size());

        for (Map.Entry<Integer, ArrayList<int[]>> entry : map.contours.entrySet()) {
            System.out.println("contour" + entry.getKey() + ", points: ");
            for (int[] i : entry.getValue()) {
                System.out.print(Arrays.toString(i));
            }
            System.out.println();
        }
    }

}






























