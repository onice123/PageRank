package one;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


/* 
 * Reads the .sql.gz files containing Wikipedia's page metadata and page links (or the cached raw data), writes out raw versions of the
 * parsed data (for faster processing next time), iteratively computes the PageRank of each page, and writes out the raw PageRank vector.
 */
public final class main {
	
	private static final File PAGE_ID_TITLE_SQL_FILE = new File("jawiki-20150512-page.sql.gz");           // Original input file
	private static final File PAGE_ID_TITLE_RAW_FILE = new File("wikipedia-pagerank-page-id-title.raw");  // For caching
	
	private static final File PAGE_LINKS_SQL_FILE = new File("jawiki-20150512-pagelinks.sql.gz");   // Original input file
	private static final File PAGE_LINKS_RAW_FILE = new File("wikipedia-pagerank-page-links.raw");  // For caching
	
	private static final File PAGERANKS_RAW_FILE = new File("wikipedia-pageranks.raw");
	
	
	public static void main(String[] args) throws IOException {
		// file and cache page-ID-title dataを読む
		Map<String,Integer> idByTitle;
		if (!PAGE_ID_TITLE_RAW_FILE.isFile()) {
			idByTitle = PageIdTitleMap.readSqlFile(PAGE_ID_TITLE_SQL_FILE);
			PageIdTitleMap.writeRawFile(idByTitle, PAGE_ID_TITLE_RAW_FILE);
		} else
			idByTitle = PageIdTitleMap.readRawFile(PAGE_ID_TITLE_RAW_FILE);
		Map<Integer,String> titleById = PageIdTitleMap.reverseMap(idByTitle);
		
		// Read file and cache page-links data
		int[] links;
		if (!PAGE_LINKS_RAW_FILE.isFile()) {
			links = PageLinksList.readSqlFile(PAGE_LINKS_SQL_FILE, idByTitle, titleById);
			PageLinksList.writeRawFile(links, PAGE_LINKS_RAW_FILE);
		} else
			links = PageLinksList.readRawFile(PAGE_LINKS_RAW_FILE);
		
		// PageRankの計算を繰り返す
		final double DAMPING = 0.85;  // Standard value is 0.85
		System.out.println("Computing PageRank...");
		Pagerank pr = new Pagerank(links);
		double[] prevPageranks = pr.pageranks.clone();
		for (int i = 0; i < 1000; i++) {
			// 繰り返す
			System.out.print("Iteration " + i);
			long startTime = System.currentTimeMillis();
			pr.iterate(DAMPING);
			System.out.printf(" (%.3f s)%n", (System.currentTimeMillis() - startTime) / 1000.0);
			
			// 計算をして統計を表示
			double[] pageranks = pr.pageranks;
			printPagerankChangeRatios(prevPageranks, pageranks);
			printTopPageranks(pageranks, titleById);
			prevPageranks = pageranks.clone();
		}
		
		// Write PageRanks to file
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(PAGERANKS_RAW_FILE)));
		try {
			for (double x : pr.pageranks)
				out.writeDouble(x);
		} finally {
			out.close();
		}
	}
	
	
	private static void printPagerankChangeRatios(double[] prevPr, double[] pr) {
		double min = Double.POSITIVE_INFINITY;
		double max = 0;
		for (int i = 0; i < pr.length; i++) {
			if (pr[i] != 0 && prevPr[i] != 0) {
				double ratio = pr[i] / prevPr[i];
				min = Math.min(ratio, min);
				max = Math.max(ratio, max);
			}
		}
		System.out.println("Range of ratio of changes: " + min + " to " + max);
	}
	
	
	private static void printTopPageranks(double[] pageranks, Map<Integer,String> titleById) {
		double[] sorted = pageranks.clone();
		Arrays.sort(sorted);
		for (int i = 0; i < 30; i++) {
			for (int j = 0; j < sorted.length; j++) {
				if (pageranks[j] == sorted[sorted.length - 1 - i]) {
					System.out.printf("  %.2f  %s%n", Math.log10(pageranks[j]), titleById.get(j));
					break;
				}
			}
		}
		for(int i = 0; i < 10; i++){
			System.out.printf("%s%n：%.2f", titleById.get(i),  Math.log10(pageranks[i]) );// スコアの上位10位のデータが表示
		}
	}
	
	
	
	private main() {}  // Not instantiable
	
}
