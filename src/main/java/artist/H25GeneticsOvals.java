package artist;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

public class H25GeneticsOvals {
	
	static int N_GEN = 20_000;
	static int N_RECTS = 50;
	static int N_POP = 500;
	static int WIDTH = 100;
	static int HEIGHT = 100;
	static double[][] weight;
	static int MUTATION_PROB_PER_1000 = 6;
	
	static final int MUTATION_Z     = 10;
	static final int MUTATION_ALPHA = 20;
	static final int MUTATION_COLOR = 20;
	static final int MUTATION_X     = 10;
	static final int MUTATION_Y     = 10;
	static final int MUTATION_H     = 10;
	static final int MUTATION_W     = 10;

	static final Random random = new Random(1982);
	
	public static class Chromosome {
		int x;
		int y;
		int w;
		int h;
		int c;
		int a;
		int z;

		public Chromosome(int x, int y, int w, int h, int c, int a, int z) {
			super();
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.c = c;
			this.a = a;
			this.z = z;
		}

		public static Chromosome generate() {
			int x = random.nextInt(WIDTH);
			int y = random.nextInt(HEIGHT);
			int w = random.nextInt(WIDTH - x);
			int h = random.nextInt(HEIGHT - y);
			int c = random.nextInt(255);
			int a = random.nextInt(255);
			int z = random.nextInt(255);
			return new Chromosome(x, y, w, h, c, a, z);
		}
		
		public Chromosome mutate() {
			if (random.nextInt(1000) < MUTATION_PROB_PER_1000) {
				int r = random.nextInt(100);
				if (r < MUTATION_Z) {
					return new Chromosome(this.x, this.y, this.w, this.h, this.c, this.a, random.nextInt(255));
				} else { r -= MUTATION_Z; }
				
				if (r < MUTATION_ALPHA) {
					return new Chromosome(this.x, this.y, this.w, this.h, this.c, random.nextInt(255), this.z);
				} else { r -= MUTATION_ALPHA; }

				if (r < MUTATION_COLOR) {
					return new Chromosome(this.x, this.y, this.w, this.h,  random.nextInt(255), this.a, this.z);
				} else { r -= MUTATION_COLOR; }
				
				if (r < MUTATION_X) {
					return new Chromosome(random.nextInt(WIDTH - this.w), this.y, this.w, this.h,  this.c, this.a, this.z);
				} else { r -= MUTATION_X; }

				if (r < MUTATION_Y) {
					return new Chromosome(this.x, random.nextInt(HEIGHT - this.h), this.w, this.h,  this.c, this.a, this.z);
				} else { r -= MUTATION_Y; }
				
				if (r < MUTATION_W) {
					return new Chromosome(this.x, this.y, random.nextInt(WIDTH - x), this.h,  this.c, this.a, this.z);
				} else { r -= MUTATION_W; }
				
				if (r < MUTATION_H) {
					return new Chromosome(this.x, this.y, this.w, random.nextInt(HEIGHT - y),  this.c, this.a, this.z);
				} else { r -= MUTATION_H; }
				
				return generate();
			}
			return new Chromosome(this.x, this.y, this.w, this.h, this.c, this.a, this.z);
		}

		public void draw(int[][] tmp) {
			double pa = (2.*x+w) / 2.;
			double pb = (2.*y+h) / 2.;
			double pX = w /2.;
			double pY = h /2.;
			
			for (int i = x; i < x+w; i++) {
				//tmp[i][y] = 255;
				//tmp[i][y+h] = 255;
				for (int j = y; j < y+h; j++) {
					//tmp[x][j] = 255;  
					//tmp[x+w][j] = 255;  
					if (sq((i-pa)/pX) + sq((j-pb)/pY) <= 1.) {
						double o = tmp[i][j];
						tmp[i][j] = (int) (c * ((a)/255.) + o *(255. - a)/255.);  
					}
				}
			}			
		}
	}
	
	public static class ADN {
		List<Chromosome> chromosomes;
		
		public ADN(List<Chromosome> chromosomes) {
			super();
			Collections.sort(chromosomes, Comparator.comparing(c -> c.z));
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
			for (int i = 0; i < WIDTH; i++) {
				for (int j = 0; j < HEIGHT; j++) {
					tmp[i][j] = 0;
				}
			}
			for (H25GeneticsOvals.Chromosome c : chromosomes) {
				c.draw(tmp);
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

	
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length == 0) throw  new RuntimeException("Argument : image file name");
		
		BufferedImage image = ImageIO.read(new File(args[0]));

		int w = image.getWidth();
		int h = image.getHeight();
		
		if (w != WIDTH || h != HEIGHT) throw new RuntimeException("Image should be " + WIDTH +"x" + HEIGHT);
		
		if (!new File("./gen").exists()) {
			new File("./gen").mkdir();
		}
		
		int[][] origine = new int[WIDTH][HEIGHT];
		weight = new double[WIDTH][HEIGHT];
		
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				origine[i][j] = new Color(image.getRGB(i, j)).getRed();
				weight[i][j] = 1.;
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
		for (int k$ = 0; k$ < N_GEN; k$++) {
			Map<ADN, Double> scores = new ConcurrentHashMap<>(); 
			
			population.stream().forEach(a -> {
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
				int a0 = random.nextInt(N_POP);
				int a1 = a0, a2 = a0, a3 = a0;
				while (a1 == a0) a1 = random.nextInt(N_POP);
				while (a2 == a0 || a2 == a1) a2 = random.nextInt(N_POP);
				while (a3 == a0 || a3 == a1 || a3 == a2) a3 = random.nextInt(N_POP);
				
				ADN parentGauche = scores.get(population$.get(a0)) < scores.get(population$.get(a1)) ? population$.get(a0) : population$.get(a1); 
				ADN parentDroite = scores.get(population$.get(a2)) < scores.get(population$.get(a3)) ? population$.get(a2) : population$.get(a3);
				
				return parentGauche.cross(parentDroite).mutate();
			})
			.collect(Collectors.toList());
			population.add(best);
			
			allbest = best;
			gen++;
		}
		
		background.shutdown();
		background.awaitTermination(1, TimeUnit.MINUTES);
		
		for (int i = 0; i < allbest.chromosomes.size(); i++) {
			Chromosome c = allbest.chromosomes.get(i);
			System.out.println(c.x + " " + c.y +" " + c.w + " " + c.h +" " + c.c + " " + c.a + " " + c.z + " " + i);
		}
		
		
	}


	private static void dump(int[][] origine, ADN best, Double score, int gen, int ndiff) throws FileNotFoundException, IOException {
		BufferedImage resultat = new BufferedImage(WIDTH * 2, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int[][] tmp = new int[WIDTH][HEIGHT];
		List<Chromosome> chromosomes = best.chromosomes;
		for (int k = 0; k < chromosomes.size(); k++) {
			H25GeneticsOvals.Chromosome c = chromosomes.get(k);
			c.draw(tmp);
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
