package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import jakarta.servlet.http.HttpSession; // Spring Boot 2系をご利用の場合は javax.servlet.http.HttpSession に変更してください

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping; // ★GetMappingをインポート追加
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
	
	// 初期ログイン画面（通常版：ポップなデザイン）
	@RequestMapping("/WebShop")
	public String Start() {
		System.out.println("Main Login Screen Requested");
		return "loginorg.html";
	}	

	// ログイン認証処理
	@PostMapping("/UserLogin")
	public String loginview(Model model, @RequestParam String userid, @RequestParam String password, HttpSession session) {
		
		// 特別ルール1：LOgIN0000が送られてきたら初期ログイン画面に戻す
		if (userid.equals("LOgIN0000")) {
			return "loginorg.html";
		}
		
		// 未入力で送信された場合は新規登録画面へ（フォームの押し間違え対策）
		if (userid.isEmpty() || password.isEmpty()) {
			return "registid.html";
		}

		// ★裏ページ判定★（特定の隠しID・パスワードでログインした場合）
		if (userid.equals("SPECIAL_GUEST") && password.equals("SECRET")) {
			System.out.println("裏ページ（ホラー画面）へようこそ...");
			model.addAttribute("name", "特別なお客様");
			model.addAttribute("address", "???");
			return "passloginorg.html"; // ホラー系のログイン成功画面へ
		}

		// 通常のCSV（TXT）認証
		int lineNo = readFile(OutputFile.location, userid);
		
		if (lineNo >= 0) { // ユーザーIDが存在する場合
			String lineData = readLineData(OutputFile.location, lineNo);
			if (lineData != null) {
				String[] userFields = lineData.split(",");
				if (userFields.length >= 3) {
					String filePassword = userFields[1]; // 2番目：パスワード
					String customerName = userFields[2]; // 3番目：名前 (okina, narita, takayanagi, Shikata など)
					
					// パスワード判定
					if (password.equals(filePassword)) {
						System.out.println("ログイン成功: " + customerName + "様");
						
						// セッションにユーザー名を保持（/toppage へ移動しても保持される）
						session.setAttribute("userName", customerName);
						
						return "redirect:/toppage"; // ポップなTOPページへ遷移
					}
				}
			}
		}
		
		// ログイン失敗時
		System.out.println("ログイン失敗: IDまたはパスワードが違います。");
		return "reloginorg.html"; 
	}
	
	// スペシャルゲスト画面からの「買い物を始める」遷移先
	@GetMapping("/secrettoppage")
	public String showSecretToppage() {
		// secrettoppage.html を表示
		return "secrettoppage.html"; 
	}	

	// 買い物を始める（ポップな商品一覧トップページ）の表示
	@RequestMapping("/toppage")
	public String toppageView(Model model, HttpSession session) {
		System.out.println("Top Page Requested");
		
		// セッションから名前を取得
		String userName = (String) session.getAttribute("userName");
		if (userName == null) {
			userName = "ゲスト"; // 直接アクセスされた等の初期値
		}
		
		// HTMLテンプレートに名前を渡す
		model.addAttribute("name", userName);
		return "toppage.html"; 
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
	public String logout(HttpSession session) {
		System.out.println("User Logged Out");
		session.invalidate(); // セッションを破棄
		return "logout.html";
	}

	// ------------------ ユーティリティメソッド群 ------------------

	// ユーザーIDが既にファイル内に存在するかどうかを判定
	public boolean CheckExistingId(String id) {
		int line = readFile(OutputFile.location, id);
		return line >= 0;
	}

	// 指定されたIDがCSVの何行目に存在するかを検索
	public int readFile(String filename, String id) {
		int rtncode = -1; // 見つからない場合は -1
		int lineCount = 0;
		File file = new File(filename);
		
		// ファイルがなければ自動作成する
		if (!file.exists()) {
			try {
				if (file.getParentFile() != null) {
					file.getParentFile().mkdirs();
				}
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

	// 会員情報をCSVに追記保存する
	public static boolean writeFile1(String textfilename, String id, String pass, String name, String adre, String mail) {
		java.io.FileWriter fw = null;
		try {
			File text = new File(textfilename);
			if (text.getParentFile() != null && !text.getParentFile().exists()) {
				text.getParentFile().mkdirs();
			}
			fw = new java.io.FileWriter(text, true); // 追記モード
			
			String line = id + "," + pass + "," + name + "," + adre + "," + mail + "\n";
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
}

// 保存先パスを保持するインナークラス
class OutputFile {
	public static String location;
}

// 認証・セッション表示用のユーザー情報保持クラス
class Info {
	public String customerID;
	public String customerName;
	public String customerAddress;
}