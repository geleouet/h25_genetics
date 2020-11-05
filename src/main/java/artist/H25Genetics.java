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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

public class H25Genetics {

	static int N_RECTS = 50;
	static int N_POP = 1000;
	static int WIDTH = 100;
	static int HEIGHT = 100;
	static int MUTATION_PROB = 2;

	static final Random random = new Random(1982);
	
	static class Chromosome {
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
			if (random.nextInt(100) < MUTATION_PROB) {
				return generate();
			}
			return new Chromosome(this.x, this.y, this.w, this.h, this.c, this.a, this.z);
		}
	}
	
	static class ADN {
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
			for (H25Genetics.Chromosome c : chromosomes) {
				for (int i = c.x; i < c.x+c.w; i++) {
					for (int j = c.y; j < c.y+c.h; j++) {
						int o = tmp[i][j];
						tmp[i][j] = (int) (c.c * (c.a)/255. + o *(255. - c.a)/255.);  
					}
				}
			}
			double e = 0;
			for (int i = 0; i < WIDTH; i++) {
				for (int j = 0; j < HEIGHT; j++) {
					double o = origine[i][j];
					e += sq((o - tmp[i][j])/(o + 0.0001));
				}
			}
			return e;
		}

		public ADN mutate() {
			return new ADN(chromosomes.stream().map(c -> c.mutate()).collect(Collectors.toList()));
		}
	}

	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) throw  new RuntimeException("Argument : image file name");
		
		BufferedImage image = ImageIO.read(new File(args[0]));

		int w = image.getWidth();
		int h = image.getHeight();
		
		if (w != WIDTH || h != HEIGHT) throw new RuntimeException("Image should be " + WIDTH +"x" + HEIGHT);
		
		if (!new File("./gen").exists()) {
			new File("./gen").mkdir();
		}
		
		int[][] origine = new int[WIDTH][HEIGHT];
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				origine[i][j] = new Color(image.getRGB(i, j)).getRed();
			}
		}
		
		List<ADN> population = IntStream.rangeClosed(1, N_POP)
				.mapToObj(__ -> ADN.generate())
				.collect(Collectors.toList());
		
		int gen = 0 ;
		for (int k$ = 0; k$ < 500; k$++) {
			System.out.println("GENERATION " + gen);
			Map<ADN, Double> scores = new HashMap<>(); 
			for (ADN a : population) {
				double eval = a.eval(origine);
				scores.put(a, eval);
			}
			
			ADN best = scores.keySet().stream().min(Comparator.comparing(k -> scores.get(k))).get();
			dump(origine, best, scores.get(best), gen);
			
			List<ADN> nexts = new ArrayList<>();
			nexts.add(best);
			
			System.out.println(scores.get(best) +"-"+  best.eval(origine)+"-"+ best.eval(origine));
			
			for (int i = 1; i < N_POP; i++) {
				int a0 = random.nextInt(N_POP);
				int a1 = a0, a2 = a0, a3 = a0;
				while (a1 == a0) a1 = random.nextInt(N_POP);
				while (a2 == a0 || a2 == a1) a2 = random.nextInt(N_POP);
				while (a3 == a0 || a3 == a1 || a3 == a2) a3 = random.nextInt(N_POP);
				
				ADN parentGauche = scores.get(population.get(a0)) < scores.get(population.get(a1)) ? population.get(a0) : population.get(a1); 
				ADN parentDroite = scores.get(population.get(a2)) < scores.get(population.get(a3)) ? population.get(a2) : population.get(a3);
				
				nexts.add(parentGauche.cross(parentDroite).mutate());
			}
			
			population = nexts;
			gen++;
		}
		
	}


	private static void dump(int[][] origine, ADN best, Double score, int gen) throws FileNotFoundException, IOException {
		BufferedImage resultat = new BufferedImage(WIDTH * 2, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		System.out.println(" " + score);
		int[][] tmp = new int[WIDTH][HEIGHT];
		for (H25Genetics.Chromosome c : best.chromosomes) {
			for (int i = c.x; i < c.x+c.w; i++) {
				for (int j = c.y; j < c.y+c.h; j++) {
					int o = tmp[i][j];
					tmp[i][j] = (int) (c.c * (c.a)/255. + o *(255. - c.a)/255.);  
		} } }
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				resultat.setRGB(i, j, new Color(tmp[i][j], tmp[i][j], tmp[i][j]).getRGB());
				resultat.setRGB(WIDTH + i, j, new Color(origine[i][j], origine[i][j], origine[i][j]).getRGB());
		} }
		
		
		Graphics2D g2 = resultat.createGraphics();
		g2.setColor(Color.black);
		g2.drawString(String.format("%03d", gen), WIDTH + 80, 99);
		g2.drawString(String.format("%03d", gen), WIDTH + 80, 97);
		g2.drawString(String.format("%03d", gen), WIDTH + 81, 98);
		g2.drawString(String.format("%03d", gen), WIDTH + 79, 98);
		g2.setColor(Color.white);
		g2.drawString(String.format("%03d", gen), WIDTH + 80, 98);
		ImageIO.write(resultat, "PNG", new FileOutputStream("./gen/h_"+String.format("%03d", gen)+".png"));
	}


	private static double sq(double d) {
		return d*d;
	}

}
