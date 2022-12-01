package com.xiaoming.gomoku.engine;

import java.util.*;

/**
 * @author xiaominghe2014@gmail.com
 * date 2022/10/18
 */
public class SimpleGomokuEngine {

    public enum Direction{
        UP(new int[]{0, -1}),
        LEFT(new int[]{-1, 0}),
        UP_RIGHT(new int[]{1, -1}),
        DOWN_RIGHT(new int[]{1, 1}),
        DOWN(new int[]{0, 1}),
        RIGHT(new int[]{1, 0}),
        DOWN_LEFT(new int[]{-1, 1}),
        UP_LEFT(new int[]{-1, -1});
        private final int[] delta;

        Direction(int[] delta) {
            this.delta = delta;
        }

        public static Direction of(int d){
            for(Direction di: Direction.values()){
                if(di.ordinal()==d){
                    return di;
                }
            }
            return null;
        }

        public int[] delta() {
            return delta;
        }
        /**
         * 反向索引
         */
        public static int reverse(int d) {
            return d ^ 0b100;
        }
    }

    public class Node{
        public int x;
        public int y;
        public int color;
        /**
         * 相邻的8个方向
         */
        public final Node[] adjacent = new Node[8];

        public Node(int x, int y){
            this.x = x;
            this.y = y;
        }
    }

    public class Board {
        public final Node[][] matrix;
        public final int boardSize;
        public Board(int boardSize){
            this.boardSize = boardSize;
            matrix = new Node[boardSize][boardSize];
            for(int y = 0; y< boardSize ; y++){
                for(int x = 0 ; x< boardSize ; x++){
                    matrix[y][x] = new Node(x,y);
                }
            }
            for(Direction d: Direction.values()){
                int[] delta = d.delta();
                for(int y = 0; y< boardSize ; y++){
                    for(int x = 0 ; x< boardSize ; x++){
                        int adjX = x + delta[0];
                        int adjY = y + delta[1];
                        if (adjX >= 0 && adjX < boardSize && adjY >= 0 && adjY < boardSize)
                            matrix[y][x].adjacent[d.ordinal()] = matrix[adjY][adjX];
                    }
                }
            }
        }
    }

    public enum Shape{
        OVER_FIVE,//长连
        FIVE,//五连
        OPEN_FOUR, //活四
        SEMI_OPEN_FOUR, //冲四
        FOUR,//死四
        OPEN_THREE, //活三
        SEMI_OPEN_THREE, //冲三
        THREE,//死三
        TWO,
        ONE,;

        public static Shape ofLength(int len,int close){
            if(len>5){
                return OVER_FIVE;
            }
            if(len==5) return FIVE;
            if(len==4 && close==0) return OPEN_FOUR;
            if(len==3 && close==0) return OPEN_THREE;
            if(len==4 && close==1) return SEMI_OPEN_FOUR;
            if(len==3 && close==1) return SEMI_OPEN_THREE;
            if(len==4 && close==2) return FOUR;
            if(len==3 && close==2) return THREE;
            if(len==2) return TWO;
            if(len==1) return ONE;
            return null;
        }
    }

    public class Operation {
        public int color;
        public int status;
        public int data;
        public int x;
        public int y;
    }

    public class MoveResult{
        public boolean success;
        public boolean end;
        public int rule;
    }

    public class Point{
        public double x;
        public double y;
        Point(double x,double y){ this.x = x; this.y = y;}

        @Override
        public boolean equals(Object p){
            if(this==p) return true;
            if(p==null) return false;
            if(p instanceof Point){
                Point o = (Point) p;
                return o.x == x && o.y == y;
            }
            return false;
        }
    }

    public static final int boardSize = 15;
    private Board board;
    public static final int WHITE = 2;
    public static final int BLACK = 1;
    public static final int EMPTY = 0;
    //0玩家 1玩家
    private int turn;
    //0 无禁手规则 1 禁手 2 Yamaguchi Rule 打点+禁手
    private int rule;
    public static final int MOVE = 0;
    public static final int ANN = 1;
    public static final int SWAP = 2;
    public static final int DOT = 3;
    public static final int CHOICE = 4;
    public static final int END = 5;
    //棋局状态 0 落子 1 声明打点数量 2 交换 3 打点 4选择打点 5结束
    private int status;

    /**
     * 五子连珠
     */
    public static final  int CONNECT5 = 5;
    /**
     * 禁手
     */
    public static final  int FORBIDDEN_33 = 3;
    public static final  int FORBIDDEN_44 = 4;
    /**
     * 长连
     */
    public static final  int CONNECT_LONG = 6;

    //结束的结果 0 side0 胜 1 side1 胜 2 和
    private int result;

    private List<Operation> history;

    private int nextColor;

    private boolean swap;
    private int dotNumber;

    private int[] dots;
    public SimpleGomokuEngine(int rule){
        this.board = new Board(15);
        this.rule = rule;
        this.status = MOVE;
        this.turn = 0;
        this.nextColor = BLACK;
        this.history=new ArrayList<>();
    }

    public boolean pass(){
        if(status!=MOVE) return false;
        if(rule>1){
            if(moveSize()<4) return false;
        }
        Operation operation = new Operation();
        operation.color = nextColor;
        operation.status = MOVE;
        history.add(operation);
        checkNext();
        return true;
    }

    public MoveResult play(int x, int y,int color){
        if(status!=MOVE || color!=nextColor) return null;
        MoveResult res = new MoveResult();
        res.end = false;
        if(x<0||y<0||x>boardSize-1||y>boardSize-1){
            res.success = false;
            return res;
        }
        Operation operation = new Operation();
        operation.x = x;
        operation.y = y;
        operation.color = color;
        operation.status = MOVE;
        Node node = board.matrix[y][x];
        if(node.color!=EMPTY){
            res.success = false;
            return res;
        }
        if(color!=BLACK && color!=WHITE) {
            res.success = false;
            return res;
        }
        board.matrix[y][x].color = color;
        int i = checkRule(x, y);
        if(i==CONNECT5){
            res.success = true;
            res.end = true;
            status = END;
            result = turn;
        }else{
            res.success = true;
            if(rule>0){
                if(color==BLACK){
                    res.rule = i;
                    if(i==FORBIDDEN_44 || i==FORBIDDEN_33 || i==CONNECT_LONG){
                        res.end = true;
                        status = END;
                        result = oppositeSide(turn);
                    }
                }
            }
        }
        history.add(operation);
        checkNext();
        return res;
    }

    public boolean ann(int n){
        if(status!=ANN) return false;
        this.dotNumber = n;
        Operation operation = new Operation();
        operation.data = n;
        operation.status = ANN;
        history.add(operation);
        checkNext();
        return true;
    }

    public boolean swap(boolean swap){
        if(status!= SWAP) return false;
        this.swap = swap;
        Operation operation = new Operation();
        operation.status = SWAP;
        history.add(operation);
        checkNext();
        return true;
    }

    public boolean dot(int[] dots){
        if(status!=DOT) return false;
        if(dots.length!=dotNumber){
            return false;
        }
        boolean b = checkDot(dots);
        if(b){
            this.dots = dots;
            Operation operation = new Operation();
            operation.status = DOT;
            history.add(operation);
            checkNext();
            return true;
        }
        return false;
    }

    public boolean choice(int idx){
        if(status!=CHOICE) return false;
        if(idx<0||idx>dots.length) return false;
        int pos = dots[idx];
        int x = pos%boardSize;
        int y = ~~(pos/boardSize);
        board.matrix[y][x].color = BLACK;
        Operation operation = new Operation();
        operation.x = x;
        operation.y = y;
        operation.status = CHOICE;
        history.add(operation);
        checkNext();
        return true;
    }

    /**
     * 检测打点是否合法
     * @param dots
     * @return
     */
    private  boolean checkDot(int[] dots){
        List<Point> all = new ArrayList<>();
        for(int i = 0 ; i < dotNumber ; i++){
            int pos = dots[i];
            int x = pos%boardSize;
            int y = ~~(pos/boardSize);
            if( board.matrix[y][x].color != EMPTY) return false;
            all.add(new Point(x,y));
        }
        List<Point> bList = new ArrayList<>();
        bList.add(new Point(history.get(0).x,history.get(0).y));
        bList.add(new Point(history.get(2).x,history.get(2).y));
        List<Point> wList = new ArrayList<>();
        wList.add(new Point(history.get(1).x,history.get(1).y));
        wList.add(new Point(history.get(5).x,history.get(5).y));
        bList.sort((a,b)->(Double.valueOf(a.x-b.x).intValue()));
        wList.sort((a,b)->(Double.valueOf(a.x-b.x).intValue()));
        List<List<Point>> symLists = new ArrayList<>();
        for(int i = 0; i< dotNumber; i ++){
            symLists.add(symmetrically(all.get(i),bList,wList));
        }
        for(int i = 0 ; i < dotNumber ; i ++){
            for(int j = 0 ; j < dotNumber ; j ++){
                if(i==j) continue;
                if(symLists.get(i).contains(all.get(j))){
                    return false;
                }
            }
        }
        return true;
    }

    private List<Point> symmetrically(Point p,List<Point> bList,List<Point> wList){
        List<Point> list = new ArrayList<>();
        Point b1 = bList.get(0);
        Point b2 = bList.get(1);
        Point w1 = wList.get(0);
        Point w2 = wList.get(1);
        Point centerB = new Point((b1.x+b2.x)/2.0,(b1.y+b2.y)/2.0);
        Point centerW = new Point((w1.x+w2.x)/2.0,(w1.y+w2.y)/2.0);
        if(centerB.x==centerW.x && centerB.y==centerW.y){
            //中心对称
            list.add(new Point(centerB.x*2-p.x,centerB.y*2-p.y));
        }
        //斜率
        double kb = (b2.y - b1.y) / (b2.x - b1.x);
        double kw = (w2.y - w1.y) / (w2.x - w1.x);
        //横轴对称
        if (w1.x == w2.x && b1.x == b2.x && centerB.y == centerW.y) {
            double x = p.x;
            double y = centerB.y * 2 - p.y;
            list.add(new Point(x,y));
        } else if (w1.x == w2.x && b1.y == b2.y && centerB.y == centerW.y && b2.y == centerW.y) {
            double x = p.x;
            double y = centerB.y * 2 - p.y;
            list.add(new Point(x,y));
        }
        else if (b1.x == b2.x && w1.y == w2.y && centerB.y == centerW.y && w2.y == centerB.y) {
            double x = p.x;
            double y = centerB.y * 2 - p.y;
            list.add(new Point(x,y));
        }
        else if (centerB.x == centerW.x && centerB.y == centerW.y && (w1.y == w2.y && b1.y == b2.y && w1.y == b1.y)) {
            double y = p.y;
            double x = centerB.x * 2 - p.x;
            list.add(new Point(x,y));
        }
        //纵轴对称
        if (w1.y == w2.y && b1.y == b2.y && centerB.x == centerW.x) {
            double y = p.y;
            double x = centerB.x * 2 - p.x;
            list.add(new Point(x,y));
        }
        else if (w1.y == w2.y && b1.x == b2.x && centerB.x == centerW.x && b2.x == centerB.x) {
            double y = p.y;
            double x = centerB.x * 2 - p.x;
            list.add(new Point(x,y));
        }

        else if (b1.y == b2.y && w1.x == w2.x && centerB.x == centerW.x && w2.x == centerB.x) {
            double y = p.y;
            double x = centerB.x * 2 - p.x;
            list.add(new Point(x,y));
        }
        else if (centerB.x == centerW.x && centerB.y == centerW.y && (w1.x == w2.x && b1.x == b2.x && w1.x == b1.x)) {
            double y = p.y;
            double x = centerB.x * 2 - p.x;
            list.add(new Point(x,y));
        }
        double kcenter = (centerW.y - centerB.y) / (centerW.x - centerB.x);
        //斜线对称
        if (kb != 0 && kb == -kw && (kcenter == 1 || kcenter == -1)) {
            //交叉斜线对称
            if (kcenter < 0) {
                //斜线右上
                if (kb < 0) {
                    double x = Math.abs(centerW.x - (p.y - centerW.y));
                    double y = Math.abs(centerW.y - (p.x - centerW.x));
                    list.add(new Point(x,y));
                }
                else {
                    double x = Math.abs(centerB.x - (p.y - centerB.y));
                    double y = Math.abs(centerB.y - (p.x - centerB.x));
                    list.add(new Point(x,y));
                }
            }
            else {
                //斜线左上
                if (kb > 0) {
                    double x = Math.abs(centerW.x + (p.y - centerW.y));
                    double y = Math.abs(centerW.y + (p.x - centerW.x));
                    list.add(new Point(x,y));
                }
                else {
                    double x = Math.abs(centerW.x + (p.y - centerW.y));
                    double y = Math.abs(centerW.y + (p.x - centerW.x));
                    list.add(new Point(x,y));
                }
            }
        }
        else if (kb == kw && kcenter == -kb) {
            //平行斜线对称
            if (kcenter < 0) {
                double x = Math.abs(centerW.x - (p.y - centerW.y));
                double y = Math.abs(centerW.y - (p.x - centerW.x));
                list.add(new Point(x,y));
            }
            else  {
                double x = Math.abs(centerW.x + (p.y - centerW.y));
                double y = Math.abs(centerW.y + (p.x - centerW.x));
                list.add(new Point(x,y));
            }
        }
        return list;
    }



    private int moveSize(){
        int size = 0;
        for(Operation op:history){
            if(op.status==MOVE){
                size++;
            }
        }
        return size;
    }

    private void checkNext(){
        if(status==MOVE){
            //检测交换规则 棋局状态 黑白黑3 落子 黑声明打点数量 白交换 落子  打点 4选择打点 5结束
            if(rule==2){
                int size = moveSize();
                if(size<3){
                    nextColor = oppositeColor(nextColor);
                    return;
                }
                if(size==3){
                    status = ANN;
                    return;
                }
                if(size==4){
                    //黑打点
                    status = DOT;
                    return;
                }
            }
            nextColor = oppositeColor(nextColor);
            turn = oppositeSide(turn);
            return;

        }
        if(status==ANN){
            turn = oppositeSide(turn);
            status = SWAP;
            return;
        }
        if(status== SWAP){
            nextColor = WHITE;
            status = MOVE;
            if(swap){
                turn = oppositeSide(turn);
            }
            return;
        }

        if(status==DOT){
            turn = oppositeSide(turn);
            status = CHOICE;
            return;
        }

        if(status==CHOICE){
            //白选择完打点。白落子
            status = MOVE;
            return;
        }
    }

    public int oppositeColor(int color){
        return color==BLACK? WHITE:BLACK;
    }

    public int oppositeSide(int side){
        return side^1;
    }


    public int checkRule(int x, int y){
        int l33 = 0;
        int l44 = 0;
        int ll = 0;
        for(int i = 0 ; i < 4 ; i++){
            Set<Shape> shape = getShape(x, y, i);
            if(shape.contains(Shape.FIVE)) return CONNECT5;
            if(shape.contains(Shape.OVER_FIVE)) ll++;
            if(shape.contains(Shape.OPEN_FOUR) || shape.contains(Shape.SEMI_OPEN_FOUR)) l44++ ;
            if(shape.contains(Shape.OPEN_THREE)) l33++ ;
        }
        if(ll>0) return CONNECT_LONG;
        if(l44>1) return FORBIDDEN_44;
        if(l33>1) return FORBIDDEN_33;
        return 0;
    }

    private Set<Shape> getShape(int x, int y, int d){
        Set<Shape> shapes = new HashSet<>();
        Node node = board.matrix[y][x];
        int color = node.color;
        if(color==EMPTY)
        return shapes;
        if(color==BLACK || color==WHITE){
            //
            int d2 = Direction.reverse(d);

            int[] res1 = {0,0,0,0,0};
            res1 = search(color, x, y, Direction.of(d), res1);
            int[] res2 = {0,0,0,0,0};
            res2 = search(color, x, y, Direction.of(d2), res2);
            int count = res1[0] + 1 + res2[0]; //总长度
            int e1 = res1[1];
            int e2 = res2[1];
            int empty = e1 + e2;//空格数目
            int close = res1[2] + res2[2];//受阻方向数目
            if(empty<2){
                int len = count - empty;
                if(len<5){
                    shapes.add(Shape.ofLength(len,close));
                    return shapes;
                }
            }
            //e1=1,e2=1

            //  1_1 1 1_1
            int l0 = res1[0]-res1[3];
            int l1 = res1[3];
            int l2 = res2[3];
            int l3 = res2[0] - res2[3];

            //取中间
            int center = l1+1+l2;

            if(center>=5){
                shapes.add(Shape.ofLength(center,close));
                return shapes;
            }
            else if(center==4){
                shapes.add(Shape.OPEN_FOUR);
            }
            else if(center==3){
                shapes.add(Shape.OPEN_THREE);
            }else {
                shapes.add(Shape.ofLength(center,close));
            }
            int left = center + l0;
            int right = center + l3;
            shapes.add(Shape.ofLength(left,res1[2]));
            shapes.add(Shape.ofLength(right,res2[2]));
            return shapes;
        }else{
            shapes.add(Shape.ONE);
            return shapes;
        }
    }


    /**
     *
     * @param color
     * @param fromX
     * @param fromY
     * @param d
     * @param res 0子数1空格数2受阻数3不算空格的数目
     * @return
     */
    private int[] search(int color,int fromX,int fromY,Direction d,int[] res){
        if(color==EMPTY) return res; //空白暂无搜索必要
        int toX = fromX+d.delta[0];
        int toY = fromY+d.delta[1];
        if(toX<0 || toY<0 || toX>boardSize-1 || toY>boardSize-1){
            res[2]++; //受阻
            return res;
        }
        int colorTo = board.matrix[toY][toX].color;
        if(colorTo==color){
            res[0]++;
            res[4]=0;
        }else if(colorTo==EMPTY){
            if(res[4]==1){
                //连续空格,当作无空格
                res[1]=0;
                return res;
            }
            res[1]++;
            //多余两个
            if(res[1]>1){
                res[1] = 1;
                return res;
            }
            //非空格结尾
            res[3]=res[0];
            res[4]=1;
        }else{
            res[2]++; //受阻
            return res;
        }
        return search(color,toX,toY,d,res);
    }

    private String boardStr(){
        String columnTag = "  ";
        for(char i = 65; i< boardSize+65 ; i ++){
            columnTag += " "+ (i<73 ? i : (char)(i+1));
        }
        columnTag += "\n";
        String boardStr = "";
        for(int i = 0 ; i < boardSize; i ++){
            boardStr += boardSize - i >9 ? ""+ (boardSize - i) : " "+(boardSize - i);
            for(int j = 0 ; j < boardSize ; j ++){
                int piece = board.matrix[i][j].color;
                if(EMPTY == piece){
                    boardStr += " .";
                }
                if(BLACK == piece){
                    boardStr += " X";
                }
                if(WHITE == piece){
                    boardStr += " O";
                }
            }
            boardStr+="\n";
        }
        return columnTag+boardStr;
    }

    public static void main(String[] args) {
        SimpleGomokuEngine game = new SimpleGomokuEngine(1);
        System.out.println(game.boardStr());
        Scanner reader = new Scanner(System.in);
        while (true){
            if(reader.hasNextLine()){
                String s = reader.nextLine();
                String[] split = s.split(",");
                try {
                    Integer x = Integer.valueOf(split[0]);
                    Integer y = Integer.valueOf(split[1]);
                    Integer color = Integer.valueOf(split[2]);
                    MoveResult result = game.play(x, y, color);
                    System.out.println(game.boardStr());
                    System.out.println(result.success);
                    System.out.println(result.rule);
                    System.out.println(result.end);
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }
        }
    }

}
