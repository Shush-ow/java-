package com.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SpringBootApplication
@Controller
public class WebShop {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;
    

    public static void main(String[] args) {
        SpringApplication.run(WebShop.class, args);
    }

    // 初期ダミーデータの自動挿入（DBが空の場合）
    @PostConstruct
    public void initData() {
        if (productRepository.count() == 0) {
            productRepository.save(new Product("P001", "Tailored Wool Jacket - Black", "OUTERWEAR", 48000 , "C:\\Users\\ktc\\Desktop\\workspace\\WebShop\\src\\main\\resources\\static\\images\\book_hondana_syodana.png"));
            productRepository.save(new Product("P002", "8-Hole Leather Boots", "FOOTWEAR", 32000 , "C:\\Users\\ktc\\Desktop\\workspace\\WebShop\\src\\main\\resources\\static\\images\\book_hondana_syodana.png"));
            productRepository.save(new Product("P003", "Solid Perfume - White Magnolia", "FRAGRANCE", 8500 , "C:\\Users\\ktc\\Desktop\\workspace\\WebShop\\src\\main\\resources\\static\\images\\book_hondana_syodana.png"));
            productRepository.save(new Product("P004", "Silver Tie Pin & Ear Cuff Set", "ACCESSORY", 14000 , "C:\\Users\\ktc\\Desktop\\workspace\\WebShop\\src\\main\\resources\\static\\images\\book_hondana_syodana.png"));
        }
    }

    @RequestMapping("/WebShop")
    public String Start() {
        return "loginorg.html";
    }

    // MySQLでの認証処理
    @PostMapping("/UserLogin")
    public String loginview(Model model, @RequestParam("userid") String userid, @RequestParam("password") String password, HttpSession session) {
    if (userid.equals("LOgIN0000")) {
            return "loginorg.html";
        }
        if (userid.isEmpty() || password.isEmpty()) {
            return "registid.html";
        }
        if ("SPECIAL_GUEST".equals(userid) && "SECRET".equals(password)) {
            System.out.println("裏ページ（ホラー画面）へようこそ...");
            model.addAttribute("name", "特別なお客様");
            
            // ★DBから商品一覧を取得してモデルに追加★
            List<Product> products = productRepository.findAll();
            model.addAttribute("products", products);
            
            return "passloginorg.html";
        }

        Optional<User> userOpt = userRepository.findById(userid);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            session.setAttribute("userid", userid);
            session.setAttribute("userName", userOpt.get().getUsername());
            return "redirect:/toppage";
        }

        return "reloginorg.html";
    }

    @GetMapping("/secrettoppage")
    public String showSecretToppage() {
        return "secrettoppage.html";
    }

    // トップページ（ソート機能付き商品一覧）
    @SuppressWarnings("unchecked")
    @RequestMapping("/toppage")
    public String toppageView(@RequestParam(value = "sort", required = false) String sort, Model model, HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        if (userName == null) userName = "ゲスト";

        Sort sortOrder = Sort.unsorted();
        if ("price_asc".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "price");
        } else if ("name".equals(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "name");
        }

        List<Product> products = productRepository.findAll(sortOrder);

        model.addAttribute("name", userName);
        model.addAttribute("products", products);
        model.addAttribute("currentSort", sort);

        // カートバッジ用件数取得
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        int cartCount = (cart != null) ? cart.stream().mapToInt(CartItem::getQuantity).sum() : 0;
        model.addAttribute("cartCount", cartCount);

        return "toppage.html";
    }
    
 // 商品詳細ページ
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable("id") String id, Model model) {
        // 商品IDから商品情報を取得
        Product product = productRepository.findById(id).orElse(null);
        // 商品が存在しない場合は商品一覧へ戻る
        if (product == null) { return "redirect:/toppage";}
        // 商品情報をHTMLに渡す
        model.addAttribute("product", product);
        return "product_detail";
    }

    // カート追加処理
    @SuppressWarnings("unchecked")
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") String productId, HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) cart = new ArrayList<>();

        Optional<Product> prodOpt = productRepository.findById(productId);
        if (prodOpt.isPresent()) {
            Product product = prodOpt.get();
            boolean exists = false;
            for (CartItem item : cart) {
                if (item.getProduct().getId().equals(productId)) {
                    item.setQuantity(item.getQuantity() + 1);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                cart.add(new CartItem(product, 1));
            }
        }
        session.setAttribute("cart", cart);
        return "redirect:/toppage";
    }

    // カート表示
    @SuppressWarnings("unchecked")
    @GetMapping("/cart")
    public String showCart(HttpSession session, Model model) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) cart = new ArrayList<>();

        int totalAmount = cart.stream().mapToInt(CartItem::getTotalPrice).sum();
        model.addAttribute("cart", cart);
        model.addAttribute("totalAmount", totalAmount);
        return "cart.html";
    }

    // カート項目削除
    @SuppressWarnings("unchecked")
    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam("productId") String productId, HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart != null) {
            cart.removeIf(item -> item.getProduct().getId().equals(productId));
            session.setAttribute("cart", cart);
        }
        return "redirect:/cart";
    }

    // 購入確定（レジ完了処理）
    @PostMapping("/checkout")
    public String checkout(HttpSession session) {
        session.removeAttribute("cart");
        return "buyaccept.html";
    }

    @RequestMapping("/registid")
    public String registidview() {
        return "registid.html";
    }

    // 会員登録（MySQLへ保存）
    @PostMapping("/RegistUserID")
    public String registerUser(
            @RequestParam("userid") String userid, 
            @RequestParam("password1") String password1, 
            @RequestParam("password2") String password2,
            @RequestParam("username") String username, 
            @RequestParam("address") String address, 
            @RequestParam("email") String email) {
    	
        if (!password1.equals(password2)) return "registpassdeny.html";
        if (userRepository.existsById(userid)) return "registidexisting.html";

        User newUser = new User(userid, password1, username, address, email);
        userRepository.save(newUser);
        return "registaccept.html";
    }
    
    // 会員情報変更(MySQLに上書き)
    @GetMapping("/UserEdit")
    public String userEdit(HttpSession session, Model model) {
        String userid = (String) session.getAttribute("userid");
        System.out.println("UserEdit userid = " + userid);
        if (userid == null) {
            return "redirect:/WebShop";
        }
        User user = userRepository.findById(userid).orElse(null);
        if (user == null) {
            return "redirect:/WebShop";
        }
        model.addAttribute("user", user);
        return "useredit.html";
    }
    
 // 会員情報変更（MySQLに上書き）
    @PostMapping("/UpdateUser")
    public String updateUser(
            Model model,
            @RequestParam("userid") String userid,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("username") String username,
            @RequestParam("address") String address,
            @RequestParam("email") String email,
            HttpSession session) {

        // 現在のユーザーを取得
        User user = userRepository.findById(userid).orElse(null);

        if (user == null) {
            return "usernotfound.html";
        }

        // 現在のパスワードが正しいか確認
        if (!user.getPassword().equals(currentPassword)) {
            model.addAttribute("errorMessage", "現在のパスワードが正しくありません。");

            // 編集画面に必要なユーザー情報を渡す
            model.addAttribute("user", user);

            return "useredit.html";
        }

        // 新しいパスワードに変更
        user.setPassword(newPassword);

        // その他のユーザー情報を変更
        user.setUsername(username);
        user.setAddress(address);
        user.setEmail(email);

        // MySQLへ保存
        userRepository.save(user);

        // セッションに保存しているユーザー名も更新
        session.setAttribute("userName", username);

        return "updateaccept.html";
    }

    @PostMapping("/UserLogout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "logout.html";
    }
}