package one;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/* 
 ①１行ごとに1つのページタイトルによるテキストファイルを読む。
 ②PageRankを降順にしてタイトルを分類する。
 ③新しいテキストファイルに書き込む。
 ただし、メインプログラムによってすでに計算されているrawデータファイルが必要。
 */
public final class sortpagetitlesbypagerank {
	
	// ユーザー：input/output files
	private static final File PAGE_TITLES_INPUT_FILE = new File("page-titles.txt");
	private static final File PAGE_TITLES_OUTPUT_FILE = new File("page-titles-sorted.txt");
	
	// 事前に計算されたデータファイル
	private static final File PAGE_ID_TITLE_RAW_FILE = new File("wikipedia-pagerank-page-id-title.raw");  // キャッシュのため
	private static final File PAGERANK_RAW_FILE = new File("wikipedia-pageranks.raw");
	
	
	public static void main(String[] args) throws IOException {
		// Read title-ID map
		Map<String,Integer> idByTitle = PageIdTitleMap.readRawFile(PAGE_ID_TITLE_RAW_FILE);
		
		// Read page titles to sort
		Set<String> titles = new HashSet<String>();
		BufferedReader in0 = new BufferedReader(new InputStreamReader(new FileInputStream(PAGE_TITLES_INPUT_FILE), "UTF-8"));
		try {
			while (true) {
				String line = in0.readLine();
				if (line == null)
					break;
				if (titles.contains(line))
					System.out.println("Duplicate removed: " + line);
				else if (!idByTitle.containsKey(line))
					System.out.println("Nonexistent page title removed: " + line);
				else
					titles.add(line);
			}
		} finally {
			in0.close();
		}
		
		// Read all PageRanks
		double[] pageranks = new double[(int)(PAGERANK_RAW_FILE.length() / 8)];
		DataInputStream in1 = new DataInputStream(new BufferedInputStream(new FileInputStream(PAGERANK_RAW_FILE)));
		try {
			for (int i = 0; i < pageranks.length; i++)
				pageranks[i] = in1.readDouble();
		} finally {
			in1.close();
		}
		
		// ソートして書き出す
		List<Entry> entries = new ArrayList<Entry>();
		for (String title : titles)
			entries.add(new Entry(pageranks[idByTitle.get(title)], title));
		Collections.sort(entries);
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(PAGE_TITLES_OUTPUT_FILE), "UTF-8"));
		try {
			for (Entry e : entries)
				out.printf("%.3f\t%s\n", Math.log10(e.pagerank), e.title);
		} finally {
			out.close();
		}
	}
	
	
	
	private static class Entry implements Comparable<Entry> {
		
		public final double pagerank;
		public final String title;
		
		
		public Entry(double pr, String tit) {
			pagerank = pr;
			title = tit;
		}
		
		
		public int compareTo(Entry other) {
			int temp = Double.compare(other.pagerank, pagerank);
			if (temp != 0)
				return temp;  // 初めに、PageRankを降順にソート
			else
				return title.compareTo(other.title);  // 次にタイトルを昇順にソート
		}
		
	}
	
}
