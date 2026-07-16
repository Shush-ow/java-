package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootApplication
@Controller
public class WebShop {
	
	public static void main(String[] args) {
		// ※ Pleiades環境のパス。必要に応じて実際のフォルダ構成に合わせて変更してください。
		OutputFile.location = "C:\\pleiades\\2025-12\\java\\Useridinfo.txt";
		SpringApplication.run(WebShop.class, args);
	}
	
	// 初期ログイン画面の表示
	@RequestMapping("/WebShop")
	public String Start() {
		System.out.println("User Login Screen Requested");
		return "loginorg.html";
	}	

	// ログイン認証処理
	@PostMapping("/UserLogin")
	public String loginview(Model model, @RequestParam String userid, @RequestParam String password)  {
		String rtnhtml = "passloginorg.html"; // ログアウトボタン付きの成功画面
		
		// 特別ルール：LOgIN0000が送られてきたら初期ログイン画面に戻す
		if (userid.equals("LOgIN0000")) {
			return "loginorg.html";
		}
		
		// 画面遷移で「ユーザID登録」ボタンが押された場合（同一formから送信されるための対策）
		// HTML側のsubmitボタンの値や、リクエストパラメータに応じて分岐する設計に合わせます
		// 念のため、ログイン失敗画面（reloginorgなど）からIDなしで新規登録が押された時の挙動もカバー
		if (userid.isEmpty() || password.isEmpty()) {
			return "registid.html";
		}

		// CSVファイルを読み込んで認証（戻り値：ファイル内の行インデックス）
		int lineNo = readFile(OutputFile.location, userid);
		
		if (lineNo >= 0) { // ユーザーIDが存在する場合
			String lineData = readLineData(OutputFile.location, lineNo);
			if (lineData != null) {
				String[] userFields = lineData.split(",");
				if (userFields.length >= 4) {
					String filePassword = userFields[1];
					String customerName = userFields[2];
					String customerAddress = userFields[3];
					
					// パスワード判定
					if (password.equals(filePassword)) {
						System.out.println("ログイン成功: " + customerName + "様");
						model.addAttribute("name", customerName);
						model.addAttribute("address", customerAddress);
						return rtnhtml; // passloginorg.html へ
					}
				}
			}
		}
		
		// ログイン失敗時
		System.out.println("ログイン失敗: IDまたはパスワードが違います。");
		return "reloginorg.html"; // 新規登録ボタン付きの再ログイン画面
	}

	// 新規登録画面の要求
	@RequestMapping("/registid")
	public String registidview() {
		return "registid.html";
	}

	// 新規会員登録処理
	@PostMapping("/RegistUserID")
	public String registerUser(
			@RequestParam String userid, 
			@RequestParam String password1, 
			@RequestParam String password2, 
			@RequestParam String username, 
			@RequestParam String address,
			@RequestParam String email) {
		
		// 1. パスワードの一致チェック
		if (!password1.equals(password2)) {
			System.out.println("新規登録エラー: パスワードが一致しません。");
			return "registpassdeny.html";
		}
		
		// 2. ユーザーIDの重複チェック
		if (CheckExistingId(userid)) {
			System.out.println("新規登録エラー: このユーザーIDは既に存在します -> " + userid);
			return "registidexisting.html";
		}
		
		// 3. ファイルへの書き込み（新規会員情報の保存）
		if (writeFile1(OutputFile.location, userid, password1, username, address, email)) {
			System.out.println("ユーザー登録成功: " + userid);
			return "registaccept.html"; // 登録完了画面
		} else {
			System.out.println("システムエラー: ファイル書き込みに失敗しました。");
			return "registid.html"; // 失敗時は入力画面に戻す
		}
	}

	// ログアウト処理
	@PostMapping("/UserLogout")
	public String logout() {
		System.out.println("User Logged Out");
		return "logout.html";
	}

	// ------------------ ユーティリティメソッド群 ------------------

	// ユーザーIDが既にファイル内に存在するかどうかを判定
	public boolean CheckExistingId(String id) {
		int line = readFile(OutputFile.location, id);
		if (line >= 0) {
			return true; // すでに存在する
		}
		return false; // 存在しない（登録可能）
	}

	// 指定されたIDがCSVの何行目に存在するかを検索
	public int readFile(String filename, String id) {
		int rtncode = -1; // 見つからない場合は -1
		int lineCount = 0;
		File file = new File(filename);
		
		// ファイルがなければ自動作成する（初回ログイン・登録エラー回避のため）
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("ファイル作成エラー: " + e);
				return -2;
			}
		}

		try (BufferedReader brt = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = brt.readLine()) != null) {
				String[] fields = line.split(",");
				if (fields.length > 0 && fields[0].equals(id)) {
					rtncode = lineCount; // 行番号をセット
					break;
				}
				lineCount++;
			}
		} catch (FileNotFoundException e) {
			System.out.println("ファイルが見つかりません: " + e);
			rtncode = -11;
		} catch (IOException e) {
			System.out.println("I/Oエラー: " + e);
			rtncode = -12;
		}
		return rtncode;
	}

	// 特定行のデータをまるごと取得するヘルパーメソッド
	private String readLineData(String filename, int targetLine) {
		try (BufferedReader brt = new BufferedReader(new FileReader(filename))) {
			String line;
			int currentLine = 0;
			while ((line = brt.readLine()) != null) {
				if (currentLine == targetLine) {
					return line;
				}
				currentLine++;
			}
		} catch (IOException e) {
			System.out.println("ファイル読み込みエラー: " + e);
		}
		return null;
	}


	public static boolean writeFile1(String textfilename, String id, String pass, String name, String adre, String mail) {
		java.io.FileWriter fw = null;
		try {
			File text = new File(textfilename);
			// フォルダが存在しない場合は作成
			if (text.getParentFile() != null && !text.getParentFile().exists()) {
				text.getParentFile().mkdirs();
			}
			fw = new java.io.FileWriter(text, true); 
			
			String line = id + "," + pass + "," + name + "," + adre + "." + mail + "\n";
			fw.write(line);
			fw.flush();
			return true;
		} catch (IOException e) {
			System.out.println("書き込みエラー: " + e);
			return false;
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					System.out.println("ファイルクローズエラー: " + e);
				}
			}
		}
	}
	

		@RequestMapping("/toppage")
		public String toppageView() {
			System.out.println("Top Page Requested");
			return "toppage.html"; 
		}
	
}

// 保存先パスを保持するインナークラス
class OutputFile {
	public static String location;
}

// 認証・セッション表示用のユーザー情報保持クラス (各処理内で安全に扱うよう変更)
class Info {
	public String customerID;
	public String customerName;
	public String customerAddress;
}