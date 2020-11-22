package artist;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class Penrose {

	static int WIDTH = 100;
	static int HEIGHT = 100;
	
	private static final double PHI = (1. + Math.sqrt(5.))/2.;
	
	static class Point {
		final double x;
		final double y;
		
		
		public static Point point(double x, double y) {
			return new Point(x, y);
		}
		
		private Point(double x, double y) {
			super();
			this.x = x;
			this.y = y;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(x);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(y);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}



		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Point other = (Point) obj;
			if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
				return false;
			if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "(" + x + ", " + y + ")";
		}

		public double dist2(Point b) {
			return sq(x - b.x) + sq(y - b.y);
		}
	}
	
	static abstract class Triangle {
		final Point a;
		final Point b;
		final Point c;
		
		@Override
		public String toString() {
			return "[" + a +", " + b +", " + c + "]";
		}
		
		public Triangle(Point a, Point b, Point c) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
		}
		
		public abstract List<Triangle> split();
		
		static ConcurrentMap<Triangle, Triangle> cache = new ConcurrentHashMap<>();
		
		static Triangle aigu(Point a, Point b, Point c) {
			Triangle r = new Aigu(a, b, c);
			Triangle prev = cache.putIfAbsent(r, r);
			return prev == null ? r : prev;
		}

		static Triangle obtus(Point a, Point b, Point c) {
			Triangle r = new Obtus(a, b, c);
			Triangle prev = cache.putIfAbsent(r, r);
			return prev == null ? r : prev;
		}
		
		@Override
		public int hashCode() {
			return a.hashCode() + 31 * b.hashCode() + 31*31 * c.hashCode();
			//return Objects.hash(a, b, c);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Triangle) {
				Triangle other = (Triangle) obj;
				return other.a.equals(a) &&
						other.b.equals(b) &&
						other.c.equals(c);
				
			}
			return false;
		}
	}
	
	static class Aigu extends Triangle {

		public Aigu(Point a, Point b, Point c) {
			super(a, b, c);
		}

		@Override
		public List<Triangle> split() {
			Point s = barycentre(a).and(c);
			//Point s = barycentre(a, PHI - 1).and(c, 2 - PHI);
			/*List<Triangle> obtusSplitted = new Obtus(b, c, s).split();
			return List.of(obtusSplitted.get(0), obtusSplitted.get(1), new Aigu(s, a, b));*/
			return List.of(obtus(b, c, s), aigu(s, a, b));
		}

	}

	static class Obtus extends Triangle {
		
		public Obtus(Point a, Point b, Point c) {
			super(a, b, c);
		}
		
		@Override
		public List<Triangle> split() {
			Point s = barycentre(b).and(a);
			//Point s = barycentre(b, PHI - 1).and(a, 2 - PHI);
			//Point s = barycentre(a, 2 - PHI).and(b, PHI - 1);
			return List.of(aigu(c, s, a), obtus(b, c, s));
		}
	}

	static class Link {
		final Node from;
		final Node to;
		
		public Link(Node from, Node to) {
			super();
			this.from = from;
			this.to = to;
		}
		
	}
	
	static class Node {
		Node left;
		Node right;
		Triangle t;
		
		public Node(Triangle triangle) {
			this.t = triangle;
		}

		public void expand() {
			if (t.a.dist2(t.b) < sq(1./10_000)) return;
			List<Triangle> split = t.split();
			left = new Node(split.get(0));
			right = new Node(split.get(1));
		}
		
		public void close() {
			left = null;
			right = null;
		}
		
		
		public void switchState(int depth) {
			if (isLeaf()) {
				class T {
					final Node n;
					final int depth;
					public T(Node n, int depth) {
						super();
						this.n = n;
						this.depth = depth;
					}
				}
				Queue<T> queue = new ArrayDeque<>();
				queue.add(new T(this, depth));
				while (!queue.isEmpty()) {
					var q = queue.poll();
					q.n.expand();
					if (!q.n.isLeaf() && q.depth > 0) {
						queue.add(new T(q.n.left, q.depth -1));
						queue.add(new T(q.n.right, q.depth -1));
					}
				}
			}
			else {
				close();
			}
		}
		
		public boolean isLeaf() {
			return left == null;
		}

		public Node cross(Node other, Random random) {
			Map<Triangle, Node> g = explode();
			Map<Triangle, Node> d = other.explode();
			
			Node res = new Node(t);
			
			Queue<Link> queue = new ArrayDeque<>();
			queue.add(new Link(this, res));
			while (!queue.isEmpty()) {
				var q = queue.poll();
				if (q.from.isLeaf()) {
					continue;
				}
				var keyG = q.from.left.t;
				if (d.containsKey(keyG) && g.containsKey(keyG)) {
					if (random.nextInt(1000) > 500) {
						var t = d.get(keyG);
						Node n = new Node(t.t);
						q.to.left = n;
						queue.add(new Link(t, n));
					}
					else {
						var t = g.get(keyG);
						Node n = new Node(t.t);
						q.to.left = n;
						queue.add(new Link(t, n));
					}
				}
				else if (d.containsKey(keyG)) {
					var t = d.get(keyG);
					Node n = new Node(t.t);
					q.to.left = n;
					queue.add(new Link(t, n));
				}
				else if (g.containsKey(keyG)) {
					var t = g.get(keyG);
					Node n = new Node(t.t);
					q.to.left = n;
					queue.add(new Link(t, n));
				}
				
				var keyD = q.from.right.t;
				if (d.containsKey(keyD) && g.containsKey(keyD)) {
					if (random.nextInt(1000) > 500) {
						var t = d.get(keyD);
						Node n = new Node(t.t);
						q.to.right = n;
						queue.add(new Link(t, n));
					}
					else {
						var t = g.get(keyD);
						Node n = new Node(t.t);
						q.to.right = n;
						queue.add(new Link(t, n));
					}
				}
				else if (d.containsKey(keyD)) {
					var t = d.get(keyD);
					Node n = new Node(t.t);
					q.to.right = n;
					queue.add(new Link(t, n));
				}
				else if (g.containsKey(keyD)) {
					var t = g.get(keyD);
					Node n = new Node(t.t);
					q.to.right = n;
					queue.add(new Link(t, n));
				}
			}
			return res;
		}

		private Map<Triangle, Node> explode() {
			Map<Triangle, Node> g = new HashMap<>();
			
			Queue<Penrose.Node> queue = new ArrayDeque<>();
			queue.add(this);
			while (!queue.isEmpty()) {
				Node q = queue.poll();
				g.put(q.t, q);
				if (!q.isLeaf()) {
					queue.add(q.left);
					queue.add(q.right);
				}
			}
			return g;
		}
		
		public Node mutate(int proba, Random random) {
			Node res = new Node(this.t);
			Queue<Link> queue = new ArrayDeque<>();
			queue.add(new Link(this, res));
			while (!queue.isEmpty()) {
				Link q = queue.poll();
				
				if (!q.from.isLeaf()) {
					Node a = new Node(q.from.left.t).shouldSwitch(random, proba);
					q.to.left = a;
					queue.add(new Link(q.from.left, a));

					Node b = new Node(q.from.right.t).shouldSwitch(random, proba);
					q.to.right = b;
					queue.add(new Link(q.from.right, b));
				}
			}
			return res;
		}
		
		private Node shouldSwitch(Random random, int proba) {
			if (random.nextInt(100_000) < proba) {
				this.switchState(1 + random.nextInt(19));
			}
			return this;
		}

		public Stream<Triangle> feuillesStream() {
			Queue<Penrose.Node> queue = new ArrayDeque<>();
			queue.add(this);
			AtomicReference<Triangle> next = new AtomicReference<>();
			return Stream.iterate((Triangle) null, __ -> next.getAndUpdate( ___ -> {
				while (!queue.isEmpty()) {
					var q = queue.poll();
					if (q.isLeaf()) {
						return q.t;
					}
					else {
						queue.add(q.left);
						queue.add(q.right);
					}
				}
				return null;})).skip(2).takeWhile(Objects::nonNull);
		}
		public List<Penrose.Triangle> feuilles() {
			List<Penrose.Triangle> toDraw = new ArrayList<>();
			Queue<Penrose.Node> queue = new ArrayDeque<>();
			queue.add(this);
			while (!queue.isEmpty()) {
				var q = queue.poll();
				if (q.isLeaf()) {
					toDraw.add(q.t);
				}
				else {
					queue.add(q.left);
					queue.add(q.right);
				}
			}
			return toDraw;
		}
	}
	
	
	static int[][] origine; 
	static double[][] weight;
	
	static double sq(double d) {return d*d;}
	public static void prepare(String filename) throws IOException {
		
		
		BufferedImage image = ImageIO.read(new File(filename));
		BufferedImage wimage = ImageIO.read(new File("h25_grey_w.png"));
		BufferedImage wwimage = ImageIO.read(new File("h25_grey_ww.png"));
		BufferedImage iimage = ImageIO.read(new File("h25_grey_i.png"));

		int w = image.getWidth();
		int h = image.getHeight();
		
		if (w != WIDTH || h != HEIGHT) throw new RuntimeException("Image should be " + WIDTH +"x" + HEIGHT);
		
		origine = new int[WIDTH][HEIGHT];
		weight = new double[WIDTH][HEIGHT];
		
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				origine[i][j] = new Color(image.getRGB(i, j)).getRed();
				weight[i][j] = 1.;
				weight[i][j] += 5*(1. - new Color(wwimage.getRGB(i, j)).getRed() / 255.);
				weight[i][j] += 1. - new Color(wimage.getRGB(i, j)).getRed() / 255.;
				weight[i][j] -= 1. - new Color(iimage.getRGB(i, j)).getRed() / 255.;
			}
		}
		for (int i = 1; i < WIDTH - 1; i++) {
			for (int j = 1; j < HEIGHT- 1; j++) {
				double s = sq(origine[i][j] - origine[i-1][j])  
				         + sq(origine[i][j] - origine[i+1][j])
				         + sq(origine[i][j] - origine[i][j-1])
				         + sq(origine[i][j] - origine[i][j+1]);
				weight[i][j] += 0.1 * (1- Math.min(1, Math.max(0, 10_000 - s) / 10_000.));
			}
		}
		for (int m = 5; m <= 10; m++) {
			for (int i = m; i < WIDTH - m; i++) {
				for (int j = m; j < HEIGHT- m; j++) {
					double s = 0;  
					for (int k = i - m; k < i + m; k++) {
						for (int l = j - m; l < j + m; l++) {
							s += sq(origine[i][j] - origine[k][l]);  
						}
					}
					weight[i][j] += 0.2 * (1 - Math.min(1, Math.max(0, 500_000. - s) / (500_000.)));
				}
			}
		}
		
		int width = 1000;
		int height = (int) (width*Math.sin(Math.PI*2./5.)*PHI);
		resultat = new BufferedImage(width, height +1, BufferedImage.TYPE_INT_ARGB);
		
		int targetWidth = 100;
		int targetHeight = (int) (height*100./width);
		outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

		g2 = resultat.createGraphics();
		go2 = outputImage.createGraphics();
		
	}
	static BufferedImage resultat ;
	static BufferedImage outputImage;
	public static void main(String[] args) throws IOException {
		//if (args.length == 0) throw  new RuntimeException("Argument : image file name");
		prepare("h25_balk.png");
		
		double y = Math.sin(Math.PI*2./5.)*PHI;
		var a = Triangle.aigu(Point.point(0, y), Point.point(1, y), Point.point(0.5, 0));

		
		int n_pop = 20;
		int TOURNAMENT_SIZE = 2;
		
		List<Node> population = new ArrayList<>();
		
		for (int i = 0; i < n_pop; i++) {
			Node root = new Node(a);
			population.add(root);
			
			Queue<Node> queue = new ArrayDeque<>();
			queue.add(root);
			int dq = 0;
			while (!queue.isEmpty() && dq < 1_000_000) {
				dq++;
				Node poll = queue.poll();
				if (Math.random()>0.2) {
					poll.expand();
					if (!poll.isLeaf() && queue.size() + dq < 1_000_000) {
						queue.add(poll.left);
						queue.add(poll.right);
					}
				}
			}
			
			System.out.println(i+":"+dq);
		}
		System.out.println("Generateds");
		
		
		int igen =0;
		while (true) 
		{
			System.out.println("\nStart eval");
			Map<Node, Double> evals = new ConcurrentHashMap<>();
			for (Node root: population) {
				//List<Triangle> toDraw = root.feuilles();

				double val = eval(root.feuillesStream(), false);
				evals.put(root, val);
			}
			Node maxi = evals.keySet().stream().min(Comparator.comparing(evals::get)).get();
			
			List<Triangle> feuilles = maxi.feuilles();
			System.out.println("Generation " + igen +" => " + evals.get(maxi) + "  (" + feuilles.size() + ")");
			eval(feuilles, true);
			draw(1000, feuilles, igen);
			igen++;
		
			System.out.println("cross/mutate");
			
			List<Node> populationNext = new ArrayList<>();
			populationNext.add(maxi);

			Triangle.cache.clear();
			List<Node> population$ = population;
			IntStream.rangeClosed(1, n_pop).parallel().forEach(__ ->
			{
				
				ThreadLocalRandom currentRandom = ThreadLocalRandom.current();
				var parentGauche = currentRandom.ints(0, n_pop).limit(TOURNAMENT_SIZE)
						.mapToObj(population$::get)
						.min(Comparator.comparing(evals::get)).get();
				
				var parentDroite = currentRandom.ints(0, n_pop).limit(TOURNAMENT_SIZE)
						.mapToObj(population$::get)
						.min(Comparator.comparing(evals::get)).get();
				
				var n = parentGauche.cross(parentDroite, currentRandom).mutate(50, currentRandom);
				populationNext.add(n);
			});
			population = populationNext;
			
			System.out.println("nexts");
		}
//		var cu = List.<Triangle> of(a);
//		for (int i = 0;i < 30; i++) {
//			Collections.shuffle(cu);
//			int s = (cu.size() /3) +1;
//			cu = cu.stream()
//					.flatMap(c->Math.random() > 0.66 ? c.split().stream() : Stream.of(c))
//					.collect(Collectors.toList());
//		}
	}
	
	static AtomicInteger counter= new AtomicInteger();
	static Graphics2D g2;
	static Graphics2D go2;
	
	private static double eval(Stream<Triangle> list, boolean debug) {
		
		int width = 1000;
		int height = (int) (width*Math.sin(Math.PI*2./5.)*PHI);
		int targetWidth = 100;
		int targetHeight = (int) (height*100./width);
		
		go2.setBackground(Color.white);
		go2.clearRect(0, 0, targetWidth , (int) (targetHeight*PHI));
		
		go2.setColor(Color.black);

		list.forEach(t -> {
			go2.setColor(Color.black);
			go2.drawPolygon(new int[] {(int) (t.a.x*targetWidth), (int) (t.b.x*targetWidth), (int) (t.c.x*targetWidth)}, new int[] {(int) (t.a.y*targetWidth), (int) (t.b.y*targetWidth), (int) (t.c.y*targetWidth)}, 3);
		});
		
		double e = 0;
		for (int x = 0; x < 100; x++) {
			for (int y = 0; y < 100; y++) {
				int cur=new Color(outputImage.getRGB(x, targetHeight-100 + y)).getRed();
				double o =      origine[x][y];
				double ev = sq((o - cur));
				ev += sq(f0(o) - f0(cur));
				ev += sq(f1(o) - f1(cur));
				ev += sq(f2(o) - f2(cur));
				ev += sq(g(o) -   g(cur));
				
				e += weight[x][y] * ev;
			}
		}
		if (debug)
			try {
				ImageIO.write(outputImage, "PNG", new FileOutputStream("./genRose/b"+String.format("%04d", counter.getAndIncrement())+".png"));
			} catch (IOException eq) {
				throw new RuntimeException(eq);
			}
		return e;
	}
	private static double evaln(Stream<Triangle> list, boolean debug) {
		
		int width = 1000;
		int height = (int) (width*Math.sin(Math.PI*2./5.)*PHI);
		int targetWidth = 100;
		int targetHeight = (int) (height*100./width);
		
		g2.setBackground(Color.white);
		g2.clearRect(0, 0, width, (int) (height*PHI));
		
		g2.setColor(Color.black);
		
		list.forEach(t -> {
			g2.setColor(Color.black);
			g2.drawPolygon(new int[] {(int) (t.a.x*width), (int) (t.b.x*width), (int) (t.c.x*width)}, new int[] {(int) (t.a.y*width), (int) (t.b.y*width), (int) (t.c.y*width)}, 3);
		});
		
		Image resultingImage = resultat.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
		outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
		
		int[][] tmp = new int[100][100];
		
		double e = 0;
		for (int x = 0; x < 100; x++) {
			for (int y = 0; y < 100; y++) {
				tmp[x][y]=new Color(outputImage.getRGB(x, targetHeight-100 + y)).getRed();
				double o =      origine[x][y];
				double ev = sq((o - tmp[x][y]));
				ev += sq(f0(o) - f0(tmp[x][y]));
				ev += sq(f1(o) - f1(tmp[x][y]));
				ev += sq(f2(o) - f2(tmp[x][y]));
				ev += sq(g(o) -   g(tmp[x][y]));
				
				e += weight[x][y] * ev;
				if (debug) {
					outputImage.setRGB(x, targetHeight-100+y, new Color(origine[x][y],0,tmp[x][y]).getRGB());
				}
			}
		}
		return e;
	}
	private static double eval(List<Triangle> list, boolean debug) {

		int width = 1000;
		int height = (int) (width*Math.sin(Math.PI*2./5.)*PHI);
		int targetWidth = 100;
		int targetHeight = (int) (height*100./width);
		
		g2.setBackground(Color.white);
		g2.clearRect(0, 0, width, (int) (height*PHI));

		g2.setColor(Color.black);

		for (int i = 0; i < list.size(); i++) {
			Triangle t = list.get(i);
			g2.setColor(Color.black);
			g2.drawPolygon(new int[] {(int) (t.a.x*width), (int) (t.b.x*width), (int) (t.c.x*width)}, new int[] {(int) (t.a.y*width), (int) (t.b.y*width), (int) (t.c.y*width)}, 3);
		}
		
		Image resultingImage = resultat.getScaledInstance(targetWidth, targetHeight, Image.SCALE_AREA_AVERAGING);
		outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
		/*
		try {
			ImageIO.write(resultat, "PNG", new FileOutputStream("./genRose/a"+String.format("%04d", counter.getAndIncrement())+".png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}*/
		if (debug)
		try {
			ImageIO.write(outputImage, "PNG", new FileOutputStream("./genRose/b"+String.format("%04d", counter.getAndIncrement())+".png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		/**/
		int[][] tmp = new int[100][100];
		
		double e = 0;
		for (int x = 0; x < 100; x++) {
			for (int y = 0; y < 100; y++) {
				tmp[x][y]=new Color(outputImage.getRGB(x, targetHeight-100 + y)).getRed();
				double o =      origine[x][y];
				double ev = sq((o - tmp[x][y]));
				ev += sq(f0(o) - f0(tmp[x][y]));
				ev += sq(f1(o) - f1(tmp[x][y]));
				ev += sq(f2(o) - f2(tmp[x][y]));
				ev += sq(g(o) -   g(tmp[x][y]));
				
				e += weight[x][y] * ev;
				if (debug) {
					outputImage.setRGB(x, targetHeight-100+y, new Color(origine[x][y],0,tmp[x][y]).getRGB());
				}
			}
		}
		if (debug)
			try {
				ImageIO.write(outputImage, "PNG", new FileOutputStream("./genRose/c"+String.format("%04d", counter.getAndIncrement())+".png"));
			} catch (IOException e2) {
				throw new RuntimeException(e2);
			}
		return e;
	}

	
private static int f0(double i) {
	return 5 * (i < 128 ? 0 : 1);
}
private static int f1(double i) {
	return 4 * (i < 120 ? 0 : 1);
}
private static int f2(double i) {
	return 4 * (i < 136 ? 0 : 1);
}
private static int g(double i) {
	return 3 * (i < 64 ? 0 : i < 128 ? 1 : i < 192 ? 2 : 3);
}

	public static void draw(int width, List<Triangle> list, int index) {
		if (!new File("./genRose").exists()) {
			new File("./genRose").mkdir();
		}
		
		int height = (int) (width*Math.sin(Math.PI*2./5.)*PHI);
		BufferedImage resultat = new BufferedImage(width, height +1, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2 = resultat.createGraphics();
		g2.setBackground(Color.white);
		g2.clearRect(0, 0, width, (int) (height*PHI));
		
		g2.setColor(Color.black);
		
		for (int i = 0; i < list.size(); i++) {
			Triangle t = list.get(i);
			
			g2.setColor((t instanceof Aigu) ? new Color(150,130,255) : Color.yellow);  
			g2.setColor(Color.black);
			//g2.fillPolygon(new int[] {(int) (t.a.x*width), (int) (t.b.x*width), (int) (t.c.x*width)}, new int[] {(int) (t.a.y*width), (int) (t.b.y*width), (int) (t.c.y*width)}, 3);

			//g2.setColor(Color.black);
			g2.drawPolygon(new int[] {(int) (t.a.x*width), (int) (t.b.x*width), (int) (t.c.x*width)}, new int[] {(int) (t.a.y*width), (int) (t.b.y*width), (int) (t.c.y*width)}, 3);
			/*
			var pb = barycentre(t.a, 0.9).and(t.b, 0.1);
			var pc = barycentre(t.a, 0.9).and(t.c, 0.1);
			g2.fillPolygon(new int[] {(int) (t.a.x*width), (int) (pb.x*width), (int) (pc.x*width)}, 
					new int[] {(int) (t.a.y*width), (int) (pb.y*width), (int) (pc.y*width)}, 3);

			var qa = barycentre(t.b, 0.9).and(t.a, 0.1);
			var qc = barycentre(t.b, 0.9).and(t.c, 0.1);
			g2.drawPolygon(new int[] {(int) (t.b.x*width), (int) (qa.x*width), (int) (qc.x*width)}, 
					new int[] {(int) (t.b.y*width), (int) (qa.y*width), (int) (qc.y*width)}, 3);
			*/

			//g2.setStroke(new BasicStroke(2.f));
			
			/*if (t instanceof Aigu) {
				g2.fillPolygon(
						new int[] {(int) (width * (t.a.x+t.c.x)/2), (int) (width * (t.b.x+t.c.x)/2), (int) (t.c.x*width)}, 
						new int[] {(int) (width * (t.a.y+t.c.y)/2), (int) (width * (t.b.y+t.c.y)/2), (int) (t.c.y*width)}, 3);
				g2.drawLine((int) (width * (t.a.x+t.c.x)/2), (int) (width * (t.a.y+t.c.y)/2), (int) (width * (t.b.x+t.c.x)/2), (int) (width * (t.b.y+t.c.y)/2));
			}
			else if (t instanceof Obtus) {
				g2.fillPolygon(
						new int[] {(int) (width * (t.a.x+t.c.x)/2), (int) (width * (t.b.x+t.a.x)/2), (int) (t.a.x*width)}, 
						new int[] {(int) (width * (t.a.y+t.c.y)/2), (int) (width * (t.b.y+t.a.y)/2), (int) (t.a.y*width)}, 3);
				g2.drawLine((int) (width * (t.a.x+t.b.x)/2), (int) (width * (t.a.y+t.b.y)/2), (int) (width * (t.a.x+t.c.x)/2), (int) (width * (t.a.y+t.c.y)/2));
			}/**/
			//g2.setStroke(new BasicStroke(1.f));

		}
		try {
			ImageIO.write(resultat, "PNG", new FileOutputStream("./genRose/m"+String.format("%04d", index)+".png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	

	static class BarycentreFunction0 {

		private Point a;
		private double pa;

		public BarycentreFunction0(Point a, double pa) {
			this.a = a;
			this.pa = pa;
		}

		public Point and(Point b, double pb) {
			double x = a.x*pa + b.x*pb;
			double y = a.y*pa + b.y*pb;
			return Point.point(x, y);
		}
		
	}
	static class BarycentreFunction {
		
		private Point a;
		
		public BarycentreFunction(Point a) {
			this.a = a;
		}

		static Map<Point, Map<Point, Point>> cache = new ConcurrentHashMap<>();
		
		public Point and(Point b) {
			return cache.computeIfAbsent(a, __ -> new ConcurrentHashMap<>())
				.computeIfAbsent(b, __ -> {
					
					double pa = PHI - 1;
					double pb = 2 - PHI;
					
					double x = a.x*pa + b.x*pb;
					double y = a.y*pa + b.y*pb;
					return Point.point(x, y);
				});
		}
		
	}
	public static BarycentreFunction barycentre(Point a) {
		return new BarycentreFunction(a);
	}
	public static BarycentreFunction0 barycentre(Point a, double d) {
		return new BarycentreFunction0(a, d);
	}
	
}
