<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="utf-8">
<title>HTMLフォームの作成</title>
<style>
    @import url('https://fonts.googleapis.com/css2?family=Creepster&family=Shippori+Mincho&display=swap');
    body {
        background-color: #050000; color: #8b0000; font-family: 'Shippori Mincho', serif;
        background-image: radial-gradient(circle, rgba(16,0,0,0.8) 0%, rgba(0,0,0,1) 80%);
        min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center;
        text-shadow: 0 0 8px rgba(139, 0, 0, 0.8); margin: 0; padding: 20px;
    }
    h2 { font-family: 'Creepster', serif; color: #ff0000; text-align: center; }
    table { width: 100%; max-width: 450px; border-collapse: collapse; background: rgba(10, 2, 2, 0.9); border: 2px solid #5a0000; box-shadow: 0 0 30px rgba(139, 0, 0, 0.6); }
    td { padding: 15px; border: 1px solid #5a0000; color: #b5b5b5; }
    td:first-child { font-weight: bold; color: #ff3333; background-color: rgba(40, 0, 0, 0.4); }
    button { width: 100%; padding: 15px; background: linear-gradient(180deg, #4a0000 0%, #1a0000 100%); border: 2px solid #8b0000; color: #ff3333; cursor: pointer; margin-top: 10px; }
    button:hover { background: #ff0000; color: #000000; box-shadow: 0 0 25px #ff0000; }
</style>
</head>
<body>
<h2>送信されたデータ</h2>
<table>
	<tr>
		<td>ユーザID</td>
		<td><?php echo htmlspecialchars($_POST["userid"] ?? '', ENT_QUOTES, 'UTF-8'); ?></td>
	</tr>
	<tr>
		<td>パスワード</td>
		<td><?php echo htmlspecialchars($_POST["password"] ?? '', ENT_QUOTES, 'UTF-8'); ?></td>
	</tr>
</table>
<br>
<button onclick="history.back()">戻る</button>
</body>
</html>