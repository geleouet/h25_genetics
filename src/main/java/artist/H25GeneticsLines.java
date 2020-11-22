package artist;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

public class H25GeneticsLines {
	
	// 5000 => 2
	
	static int N_RECTS = 1000; 
	static int LINEW = 10;
	static int N_POP = 1000;
	static int WIDTH = 100;
	static int HEIGHT = 100;
	static double[][] weight;
	static int MUTATION_PROB_PER_1000 = 6;
	static int TOURNAMENT_SIZE = 8;
	
	static final int MUTATION_Z     = 10;
	static final int MUTATION_ALPHA = 20;
	static final int MUTATION_COLOR = 20;
	static final int MUTATION_X     = 10;
	static final int MUTATION_Y     = 10;
	static final int MUTATION_H     = 10;
	static final int MUTATION_W     = 10;

	static final Random random = new Random(1982);

	public static class World {
		int[][] tmp;
		BitSet bs = new BitSet(256*256);
		int a = 0;
		
		public World(int[][] tmp) {
			super();
			this.tmp = tmp;
			for (int x = 0; x < WIDTH; x++) {
				for (int y = 0; y < HEIGHT; y++) {
					tmp[x][y]=255;
				}
			}
		}
		
		
	}
	
	public static class Chromosome {
		int a;
		
		public Chromosome(int a) {
			super();
			this.a = a;
		}

		public static Chromosome generate() {
			int a = random.nextInt(256);
			return new Chromosome(a);
		}
		
		public Chromosome mutate() {
			if (random.nextInt(1000) < MUTATION_PROB_PER_1000) {
				return generate();
			}
			return new Chromosome(this.a);
		}

		public void draw(World world) {
			int from = world.a; 			
			int to = a;
			
			if (world.bs.get(from * 256 + to)) {
				return;
			}
			world.bs.set(from * 256 + to);
			
			double from_x = (Math.cos(2*Math.PI*from/255.))*((WIDTH-1)/2.) + WIDTH/2.; 
			double from_y = (Math.sin(2*Math.PI*from/255.))*((HEIGHT-1)/2.) + HEIGHT/2.; 
			double to_x = (Math.cos(2*Math.PI*to/255.))*((WIDTH-1)/2.) + WIDTH/2.; 
			double to_y = (Math.sin(2*Math.PI*to/255.))*((HEIGHT-1)/2.) + HEIGHT/2.;
			
			double x = from_x;
			double y = from_y;
			if (from_x == to_x) {
				if (from_y < to_y) {
					while (y < to_y +1) {
						set(world, x, y);
						y+=1;
					}
				}
				else {
					if (from_y > to_y) {
						while (y > to_y -1) {
							set(world, x, y);
							y-=1;
						}
					}
				}
			} 
			else if (from_y == to_y) {
				if (from_x < to_x) {
					while (x < to_x +1) {
						set(world, x, y);
						x+=1;
					}
				}
				else if (from_x > to_x) {
					while (x > to_x -1) {
						set(world, x, y);
						x-=1;
					}
				}
			}
			else {
				double alpha = (to_y - from_y)/(to_x - from_x);
				double beta = 1./alpha;

				if (from_x < to_x && Math.abs(alpha) < 1) {
					while (x < to_x +1) {
						set(world, x, y);
						x+=1;
						y+=alpha;
					}
				}
				else if (from_x > to_x && Math.abs(alpha) < 1) {
					while (x > to_x -1) {
						set(world, x, y);
						x-=1;
						y-=alpha;
					}
				}
				else if (from_y < to_y && Math.abs(alpha) > 1) {
					while (y < to_y +1) {
						set(world, x, y);
						x+=beta;
						y+=1;
					}
				}
				else if (from_y > to_y && Math.abs(alpha) > 1) {
					while (y > to_y -1) {
						set(world, x, y);
						x-=beta;
						y-=1;
					}
				}
			}
			
			world.a = to;
		}

		private void set(World world, double x, double y) {
			world.tmp[valideWIDTH(x)][valideHEIGHT(y)] = Math.max(0, world.tmp[valideWIDTH(x)][valideHEIGHT(y)] - LINEW);
		}

		private int valideWIDTH(double x) {
			return Math.min(WIDTH-1, Math.max(0, (int) Math.round(x)));
		}

		private int valideHEIGHT(double x) {
			return Math.min(HEIGHT-1, Math.max(0, (int) Math.round(x)));
		}
	}
	
	public static class ADN {
		List<Chromosome> chromosomes;
		
		public ADN(List<Chromosome> chromosomes) {
			super();
			//Collections.sort(chromosomes, Comparator.comparing(c -> c.z));
			this.chromosomes = chromosomes;
		}

		public static ADN generate() {
			return new ADN(IntStream.rangeClosed(1, N_RECTS)
					.mapToObj(__ -> Chromosome.generate())
					.collect(Collectors.toList()));
		}
		
		public ADN cross(ADN other) {
			List<Chromosome> list = new ArrayList<Chromosome>();
			int cutPoint = 1 + random.nextInt(N_RECTS-1);
			for (int i = 0; i < N_RECTS; i++) {
				if (i < cutPoint) {
					list.add(this.chromosomes.get(i));
				}
				else {
					list.add(other.chromosomes.get(i));
				}
			}
			return new ADN(list);
		}
		
		public double eval(int[][] origine) {
			int[][] tmp = new int[WIDTH][HEIGHT];
			World world = new World(tmp);
			
			for (H25GeneticsLines.Chromosome c : chromosomes) {
				c.draw(world);
			}
			double e = 0;
			for (int i = 0; i < WIDTH; i++) {
				for (int j = 0; j < HEIGHT; j++) {
					double o = origine[i][j];
					//e += sq((o - tmp[i][j])/(o + tmp[i][j] + 0.0001));
					double ev = sq((o - tmp[i][j]));
					ev += sq(f0(o) - f0(tmp[i][j]));
					ev += sq(f1(o) - f1(tmp[i][j]));
					ev += sq(f2(o) - f2(tmp[i][j]));
					ev += sq(g(o) - g(tmp[i][j]));
					
					e += weight[i][j] * ev;
				}
			}
			return e;
		}

		private int f0(double i) {
			return 5 * (i < 128 ? 0 : 1);
		}
		private int f1(double i) {
			return 4 * (i < 120 ? 0 : 1);
		}
		private int f2(double i) {
			return 4 * (i < 136 ? 0 : 1);
		}
		private int g(double i) {
			return 3 * (i < 64 ? 0 : i < 128 ? 1 : i < 192 ? 2 : 3);
		}

		public ADN mutate() {
			return new ADN(chromosomes.stream().map(c -> c.mutate()).collect(Collectors.toList()));
		}
	}

	
	static volatile boolean stopAll = false;
	
	private static void finalPicture(int width, int height) throws FileNotFoundException, IOException {
		finalPicture(width, height, restoreChromosomes());
	}
	private static void finalPicture(int width, int height, List<Chromosome> chromosomes) throws IOException, FileNotFoundException {
		BufferedImage resultat = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics = (Graphics2D) resultat.getGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		graphics.setBackground(Color.white);
		graphics.clearRect(0, 0, width, height);
		
		int from = 0; 			
		double dist = 0;
		graphics.setColor(Color.black);
		for (H25GeneticsLines.Chromosome c : chromosomes) {
			int to = c.a;
			
			double from_x = (Math.cos(2*Math.PI*from/255.))*((width-1)/2.) + width/2.; 
			double from_y = (Math.sin(2*Math.PI*from/255.))*((height-1)/2.) + height/2.; 
			double to_x = (Math.cos(2*Math.PI*to/255.))*((width-1)/2.) + width/2.; 
			double to_y = (Math.sin(2*Math.PI*to/255.))*((height-1)/2.) + height/2.;
			
			dist += Math.sqrt(sq(from_x-to_x) + sq(from_y-to_y));
			
			graphics.drawLine((int) Math.round(from_x), 
					(int) Math.round(from_y), 
					(int) Math.round(to_x), 
					(int) Math.round(to_y));
			
			from = to;
		}
		System.out.println(dist);
		ImageIO.write(resultat, "PNG", new FileOutputStream("./gen/a.png"));
	}
	private static List<Chromosome> restoreChromosomes() throws FileNotFoundException {
		List<Chromosome> chromosomes = new ArrayList<>();
		
		Scanner scan = new Scanner(new File("./gen/res.txt"));
		while (scan.hasNext())
			chromosomes.add(new Chromosome(scan.nextInt()));
		return chromosomes;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		finalPicture(2000, 2000);
	}
	
	
	public static void main0(String[] args) throws IOException, InterruptedException {
		if (args.length == 0) throw  new RuntimeException("Argument : image file name");
		
		
		BufferedImage image = ImageIO.read(new File(args[0]));
		BufferedImage wimage = ImageIO.read(new File("h25_grey_w.png"));
		BufferedImage wwimage = ImageIO.read(new File("h25_grey_ww.png"));
		BufferedImage iimage = ImageIO.read(new File("h25_grey_i.png"));

		int w = image.getWidth();
		int h = image.getHeight();
		
		if (w != WIDTH || h != HEIGHT) throw new RuntimeException("Image should be " + WIDTH +"x" + HEIGHT);
		
		if (!new File("./gen").exists()) {
			new File("./gen").mkdir();
		}
		new Thread(() -> {
			new Scanner(System.in).nextLine();
			stopAll= true;
		}, "bgWait").start();
		
		int[][] origine = new int[WIDTH][HEIGHT];
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
		
		ExecutorService background = Executors.newFixedThreadPool(1, r -> new Thread(r, "Dump"));
		
		List<ADN> population = IntStream.rangeClosed(1, N_POP)
				.mapToObj(__ -> ADN.generate())
				.collect(Collectors.toList());
		
		int gen = 0 ;
		int ndiff = 0;
		ADN allbest = null;
		while (!stopAll)
		{
			
			Map<ADN, Double> scores = new ConcurrentHashMap<>(); 
			
			population.parallelStream().forEach(a -> {
				scores.put(a, a.eval(origine));
			});
			
			ADN best = scores.keySet().stream().min(Comparator.comparing(k -> scores.get(k))).get();
			System.out.println("GENERATION " + gen + "// " + scores.get(best));
			
			if (best != allbest) {
				/*for (int i = 0; i < best.chromosomes.size(); i++) {
					Chromosome c = best.chromosomes.get(i);
					System.out.println(c.x + " " + c.y +" " + c.w + " " + c.h +" " + c.c + " " + c.a + " " + c.z + " " + i);
				}*/
				
				int gen$ = gen;
				int ndiff$ = ndiff;
				ndiff++;
				background.submit(() -> {
					try {
						dump(origine, best, scores.get(best), gen$, ndiff$);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
				}, "");
			}
			
			
			List<ADN> population$ = population;
			population = IntStream.range(1, N_POP)
			.parallel()		
			.mapToObj(__ -> {
				
				ThreadLocalRandom currentRandom = ThreadLocalRandom.current();
				ADN parentGauche = currentRandom.ints(0, N_POP).distinct().limit(TOURNAMENT_SIZE)
						.mapToObj(population$::get)
						.min(Comparator.comparing(scores::get)).get();

				ADN parentDroite = currentRandom.ints(0, N_POP).distinct().limit(TOURNAMENT_SIZE)
						.mapToObj(population$::get)
						.min(Comparator.comparing(scores::get)).get();
				
				return parentGauche.cross(parentDroite).mutate();
			})
			.collect(Collectors.toList());
			population.add(best);
			
			allbest = best;
			gen++;
		}
		
		
		FileWriter fw  = new FileWriter(new File("./gen/res.txt"));
		for (int i = 0; i < allbest.chromosomes.size(); i++) {
			Chromosome c = allbest.chromosomes.get(i);
			fw.write(c.a + " ");
			//System.out.println(c.x + " " + c.y +" " + c.w + " " + c.h +" " + c.c + " " + c.a + " " + c.z + " " + i);
		}
		fw.flush();
		fw.close();

		finalPicture(1000, 1000, allbest.chromosomes);
		
		background.shutdown();
		background.awaitTermination(1, TimeUnit.MINUTES);
	}


	private static void dump(int[][] origine, ADN best, Double score, int gen, int ndiff) throws FileNotFoundException, IOException {
		BufferedImage resultat = new BufferedImage(WIDTH * 2, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int[][] tmp = new int[WIDTH][HEIGHT];
		World world = new World(tmp);
		List<Chromosome> chromosomes = best.chromosomes;
		for (int k = 0; k < chromosomes.size(); k++) {
			H25GeneticsLines.Chromosome c = chromosomes.get(k);
			c.draw(world);
		}
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				resultat.setRGB(i, j, new Color(tmp[i][j], tmp[i][j], tmp[i][j]).getRGB());
				resultat.setRGB(WIDTH + i, j, new Color(origine[i][j], origine[i][j], origine[i][j]).getRGB());
		} }
		
		
		Graphics2D g2 = resultat.createGraphics();
		g2.setColor(Color.black);
		g2.drawString(String.format("%03d", gen), WIDTH + 60, 99);
		g2.drawString(String.format("%03d", gen), WIDTH + 60, 97);
		g2.drawString(String.format("%03d", gen), WIDTH + 61, 98);
		g2.drawString(String.format("%03d", gen), WIDTH + 59, 98);
		g2.setColor(Color.white);
		g2.drawString(String.format("%03d", gen), WIDTH + 60, 98);
		ImageIO.write(resultat, "PNG", new FileOutputStream("./gen/h_"+String.format("%04d", ndiff)+".png"));
	}


	private static double sq(double d) {
		return d*d;
	}

}
