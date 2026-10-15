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
    public String loginview(Model model, @RequestParam String userid, @RequestParam String password, HttpSession session) {
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

        if (userOpt.isPresent()
                && userOpt.get().getPassword().equals(password)) {

            User user = userOpt.get();

            // ログイン中のユーザー情報をセッションに保存
            session.setAttribute("userid", user.getUserid());
            session.setAttribute("userName", user.getUsername());

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
    
 // 決済ページへ移動
    @SuppressWarnings("unchecked")
    @GetMapping("/payment")
    public String payment(HttpSession session, Model model) {
    	
        List<CartItem> cart =
                (List<CartItem>) session.getAttribute("cart");

        if (cart == null || cart.isEmpty()) {
            return "redirect:/cart";
        }

        int totalAmount = cart.stream()
                .mapToInt(CartItem::getTotalPrice)
                .sum();

        model.addAttribute("cart", cart);
        model.addAttribute("totalAmount", totalAmount);

        // ログイン中のユーザーIDを取得
        String userid = (String) session.getAttribute("userid");

        if (userid == null) {
            return "reloginorg.html";
        }

        // MySQLから会員情報を取得
        Optional<User> userOpt = userRepository.findById(userid);

        if (userOpt.isEmpty()) {
            return "reloginorg.html";
        }

        model.addAttribute("user", userOpt.get());

        return "payment.html";
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
            @RequestParam String userid, @RequestParam String password1, @RequestParam String password2,
            @RequestParam String username, @RequestParam String address, @RequestParam String email) {

        if (!password1.equals(password2)) return "registpassdeny.html";
        if (userRepository.existsById(userid)) return "registidexisting.html";

        User newUser = new User(userid, password1, username, address, email);
        userRepository.save(newUser);
        return "registaccept.html";
    }

    @PostMapping("/UserLogout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "logout.html";
    }
}