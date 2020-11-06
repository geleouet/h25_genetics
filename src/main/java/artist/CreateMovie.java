package artist;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.imageio.ImageIO;

import artist.H25Genetics.ADN;
import artist.H25Genetics.Chromosome;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.ArgType;
import ws.schild.jave.encode.EncodingArgument;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.ValueArgument;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;

public class CreateMovie {

	private static String frmrte(EncodingAttributes ea) {
		return "" + 50;
	}

	
	static {
		try {
			Field go = Encoder.class.getDeclaredField("globalOptions");
			go.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<EncodingArgument> objs = (List<EncodingArgument>) go.get(Encoder.class);
			objs.add(0, new ValueArgument(ArgType.INFILE, "-r", ea -> Optional.ofNullable(frmrte(ea))));
			System.out.println();
		} catch (NoSuchFieldException | SecurityException |IllegalArgumentException|IllegalAccessException e) {
			// Silently fail
			e.printStackTrace();
		} 
	}
	
	static String s = "0 77 44 22 26 131 9 0 \n19 29 38 38 160 174 19 1 \n10 68 62 31 170 23 22 2 \n24 4 18 95 191 190 27 3 \n26 0 33 4 107 244 27 4 \n22 53 26 46 252 161 38 5 \n0 34 52 36 115 127 39 6 \n20 17 73 46 183 143 44 7 \n41 12 51 17 240 106 46 8 \n23 12 44 49 232 177 62 9 \n4 9 9 15 45 251 63 10 \n22 11 47 49 240 133 69 11 \n40 97 29 2 211 252 85 12 \n0 6 10 69 54 158 89 13 \n49 5 44 44 49 162 94 14 \n47 0 32 17 97 139 94 15 \n55 0 44 45 45 110 94 16 \n93 35 6 46 163 225 95 17 \n41 13 31 20 241 111 95 18 \n40 39 15 20 61 254 96 19 \n21 40 26 31 252 177 102 20 \n23 53 15 46 254 252 110 21 \n0 56 33 13 130 108 123 22 \n31 27 13 11 14 254 126 23 \n0 29 62 9 0 94 128 24 \n0 57 29 14 119 110 140 25 \n23 50 29 16 0 198 150 26 \n39 82 32 17 177 172 155 27 \n42 11 34 29 203 59 168 28 \n21 10 62 62 165 61 169 29 \n77 56 16 17 250 254 182 30 \n25 7 20 6 234 147 186 31 \n21 15 23 68 159 104 187 32 \n29 34 11 10 1 97 190 33 \n30 14 32 12 235 132 197 34 \n17 74 10 6 47 82 201 35 \n41 53 40 16 1 113 201 36 \n51 31 23 12 9 161 201 37 \n18 48 53 8 179 135 203 38 \n23 4 52 77 141 92 206 39 \n74 31 25 46 61 228 212 40 \n89 40 10 11 251 72 220 41 \n74 75 25 24 28 191 233 42 \n53 82 24 10 12 170 234 43 \n53 36 15 3 213 123 237 44 \n29 77 32 17 113 117 243 45 \n33 87 29 12 15 129 243 46 \n35 70 30 12 108 133 246 47 \n26 83 24 6 153 166 248 48 \n0 0 77 97 134 53 254 49";
	
	public static void main(String[] args) {
		Scanner sc = new Scanner(s);
		
		List<Chromosome> list = new ArrayList<>();
		for (int i = 0; i <50; i++) {
			int x= sc.nextInt();
			int y= sc.nextInt();
			int w= sc.nextInt();
			int h= sc.nextInt();
			int c= sc.nextInt();
			int a= sc.nextInt();
			int z= sc.nextInt();
			sc.nextInt();
			
			list.add(new Chromosome(x, y, w, h, c, a, z));
		}
		sc.close();
		
		ADN adn = new ADN(list);
		new CreateMovie().generate(adn);
		// ffmpeg -r 50 -i './m_%4d.png' out.mp4

	}

	
	public void generate(ADN adn) {
		int STEPS = 50;
		int WIDTH = 100;
		int HEIGHT = 100;
		/**/
		int n = adn.chromosomes.size();
		
		for (int i = 0; i < n; i++) {
			int a$ = adn.chromosomes.get(i).a;
			int c$ = adn.chromosomes.get(i).c;
			int x$ = adn.chromosomes.get(i).x;
			int w$ = adn.chromosomes.get(i).w;
			int y$ = adn.chromosomes.get(i).y;
			int h$ = adn.chromosomes.get(i).h;
			for (int j = 0; j < STEPS; j++) {
				double j$ = j;
				adn.chromosomes.get(i).c = (int) (255.*(STEPS-j$)/STEPS + j$/STEPS*( c$ ));
				adn.chromosomes.get(i).a = (int) (0.*(STEPS-j$)/STEPS + j$/STEPS*( a$ ));
				adn.chromosomes.get(i).x = (int) (0.*(STEPS-j$)/STEPS + j$/STEPS*( x$ ));
				adn.chromosomes.get(i).w = (int) (WIDTH*(STEPS-j$)/STEPS + j$/STEPS*( w$ ));
				adn.chromosomes.get(i).y = (int) (0.*(STEPS-j$)/STEPS + j$/STEPS*( y$ ));
				adn.chromosomes.get(i).h = (int) (HEIGHT*(STEPS-j$)/STEPS + j$/STEPS*( h$ ));
				
				draw(WIDTH, HEIGHT, adn, i, i*STEPS + j);
			}
		}
		for (int j = 0; j < STEPS; j++) {
			draw(WIDTH, HEIGHT, adn, n, n*STEPS + j);
		}
		
		/**/
		try {
			String filename ="./gen/o2.mp4";

			Encoder encoder = new Encoder();
			EncodingAttributes attrs = new EncodingAttributes();
			VideoAttributes videoAttributes = new VideoAttributes();
			videoAttributes.setFrameRate(50);
			attrs.setVideoAttributes(videoAttributes);

			MultimediaObject multimediaObject0 = new MultimediaObject(new File("./gen/h_0000.png"));
			MultimediaInfo info = multimediaObject0.getInfo();
			info.getVideo().setFrameRate(50.f);

			MultimediaObject multimediaObject = new MultimediaObject(new File("./gen/h_%04d.png")) {
				@Override
				public MultimediaInfo getInfo() {
					return info;
				}
			};
			encoder.encode(multimediaObject , new File(filename), attrs);

		} catch (IllegalArgumentException | EncoderException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public void draw(int WIDTH, int HEIGHT, ADN adn, int w, int index) {
		BufferedImage resultat = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int[][] tmp = new int[WIDTH][HEIGHT];
		List<Chromosome> chromosomes = adn.chromosomes;
		for (int k = 0; k <= w && k < chromosomes.size(); k++) {
			H25Genetics.Chromosome c = chromosomes.get(k);
			for (int i = c.x; i < c.x+c.w; i++) {
				for (int j = c.y; j < c.y+c.h; j++) {
						int o = tmp[i][j];
						tmp[i][j] = (int) (c.c * (c.a)/255. + o *(255. - c.a)/255.);
		} }
		}
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				resultat.setRGB(i, j, new Color(tmp[i][j], tmp[i][j], tmp[i][j]).getRGB());
		} }
		
		
		Graphics2D g2 = resultat.createGraphics();
		g2.setColor(Color.black);
		g2.drawString(String.format("%03d", w), 70, 99);
		g2.drawString(String.format("%03d", w), 70, 97);
		g2.drawString(String.format("%03d", w), 71, 98);
		g2.drawString(String.format("%03d", w), 69, 98);
		g2.setColor(Color.white);
		g2.drawString(String.format("%03d", w), 70, 98);
		try {
			ImageIO.write(resultat, "PNG", new FileOutputStream("./gen/m_"+String.format("%04d", index)+".png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
