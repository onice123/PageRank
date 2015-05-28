package one;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


// テキスト入力ストリームからSQL"INSERT INTO"文を解析
final class SqlReader {
	
	private final String prefix;
	private final String suffix;
	
	private BufferedReader in;
	
	
	
	public SqlReader(BufferedReader in, String tableName) {
		this.in = in;
		prefix = "INSERT INTO `" + tableName + "` VALUES ";
		suffix = ";";
	}
	
	
	
	// ストリームの最後でtuplesのリストかnullを返す
	public List<List<Object>> readInsertionTuples() throws IOException {
		while (true) {
			String line = in.readLine();
			if (line == null)
				return null;
			else if (line.equals("") || line.startsWith("--"))  // SQLのコメント行。lineの値が空欄または、文字列が--で始まる場合
				continue;// スキップ
			else if (!(line.startsWith(prefix) && line.endsWith(suffix)))  // その他のSQL行。lineの接頭辞または、接尾辞ではない場合
				continue;// スキップ
			
			// 現在の行の形："INSERT into `tablename` VALUES (...),(...),...,(...);"
			return parseTuples(line.substring(prefix.length(), line.length() - 1));// テーブル名とprefix.length()からline.length() - 1の部分的な文字列を返す
		}
	}
	
	
	public void close() throws IOException {
		in.close();
	}
	
	
	private static List<List<Object>> parseTuples(String line) {
		List<List<Object>> result = new ArrayList<List<Object>>();
		
		// Finite-state machine (ugly)
		int state = 0;
		List<Object> tuple = new ArrayList<Object>();
		int tokenStart = -1;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);// 文字列lineからi番目の文字列を返す。cとする
			switch (state) {
				// 外部のtuple,'('かどうかを判定
				case 0:
					if (c == '(')
						state = 1;
					else
						throw new IllegalArgumentException();
					break;
				
				// 内部のtuple,項目（アイテム）か終わりかを判定
				case 1:
					if (c >= '0' && c <= '9' || c == '-' || c == '.')
						state = 2;
					else if (c == '\'')
						state = 3;
					else if (c == 'N')
						state = 5;
					else if (c == ')') {
						result.add(tuple);
						tuple = new ArrayList<Object>();
						state = 8;
					} else
						throw new IllegalArgumentException();
					tokenStart = i;
					if (state == 3)
						tokenStart++;
					break;
				
				// ナンバーを加える
				case 2:
					if (c >= '0' && c <= '9' || c == '-' || c == '.');
					else if (c == ',' || c == ')') {
						String s = line.substring(tokenStart, i);// lineの、tokenStart（数値）からi（数値）の位置の文字列
						tokenStart = -1;
						if (s.indexOf(".") == -1)// sに"."が存在しない場合
							tuple.add(new Integer(s));
						else
							tuple.add(new Double(s));
						if (c == ',')
							state = 7;
						else if (c == ')') {
							result.add(tuple);
							tuple = new ArrayList<Object>();
							state = 8;
						}
					} else
						throw new IllegalArgumentException();
					break;
				
				// stringを加える
				case 3:
					if (c == '\'') {
						String s = line.substring(tokenStart, i);
						tokenStart = -1;
						if (s.indexOf('\\') != -1)
							s = s.replaceAll("\\\\(.)", "$1");  // Unescape backslashed characters
						else
							s = new String(s);  // For Java below version 7.0 update 6
						tuple.add(s);
						state = 6;
					} else if (c == '\\')
						state = 4;
					break;
				
				// '\'の後に急にstringを加える
				case 4:
					if (c == '\'' || c == '"' || c == '\\')
						state = 3;
					else
						throw new IllegalArgumentException();
					break;
				
				// 引用の終わっていない符号を加える
				case 5:
					if (c >= 'A' && c <= 'Z');
					else if (c == ',' || c == ')') {
						if (line.substring(tokenStart, i).equals("NULL"))
							tuple.add(null);
						else
							throw new IllegalArgumentException();
						tokenStart = -1;
						if (c == ',')
							state = 7;
						else if (c == ')') {
							result.add(tuple);
							tuple = new ArrayList<Object>();
							state = 8;
						}
					} else
						throw new IllegalArgumentException();
					break;
				
				// 内部のtuple,コンマか')'かを判定
				case 6:
					if (c == ',')
						state = 7;
					else if (c == ')') {
						result.add(tuple);
						tuple = new ArrayList<Object>();
						state = 8;
					} else
						throw new IllegalArgumentException();
					break;
				
				// 内部のtuple,項目（アイテム）かを判定
				case 7:
					if (c >= '0' && c <= '9' || c == '-' || c == '.')
						state = 2;
					else if (c == '\'')
						state = 3;
					else if (c == 'N')
						state = 5;
					else
						throw new IllegalArgumentException();
					tokenStart = i;
					if (state == 3)
						tokenStart++;
					break;
				
				// 外部,','か終わりかを判定
				case 8:
					if (c == ',')
						state = 9;
					else
						throw new IllegalArgumentException();
					break;
				
				// 外部,'('なのかを判定
				case 9:
					if (c == '(')
						state = 1;
					else
						throw new IllegalArgumentException();
					break;
				
				default:
					throw new AssertionError();
			}
		}
		if (state != 8)
			throw new IllegalArgumentException();
		
		return result;
	}
	
}
