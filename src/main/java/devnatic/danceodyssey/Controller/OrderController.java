package devnatic.danceodyssey.Controller;

import com.lowagie.text.Image;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import devnatic.danceodyssey.DAO.Entities.*;
import devnatic.danceodyssey.DAO.Repositories.CartRepository;
import devnatic.danceodyssey.DAO.Repositories.OrderLineRepository;
import devnatic.danceodyssey.DAO.Repositories.OrdersRepositppry;
import devnatic.danceodyssey.Interfaces.ICartService;
import devnatic.danceodyssey.Interfaces.IOrderLineService;
import devnatic.danceodyssey.Interfaces.IOrdersService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.webjars.NotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/order")
@AllArgsConstructor
@Slf4j
public class OrderController {
    private  final IOrderLineService iOrderService;
    private  final IOrdersService iOrdersService;
    private  final OrdersRepositppry ordersRepositppry;
    private  final OrderLineRepository orderLineRepository;
    private  final ICartService cartService;

    @PostMapping("/addorder")
    public OrderLine addProductsToOrderAndCreateCart(@RequestParam int nbrProduct, @RequestParam int productId, @RequestParam int idCart) {
        return iOrderService.addProductToOrderAndCreateCart(productId, nbrProduct, idCart);
    }
    @PutMapping("/{orderLineId}/quantity")
    public OrderLine updateOrderLineQuantity(@PathVariable Integer orderLineId,
                                             @RequestParam Integer newQuantity) {
        return iOrderService.updateOrderLineQuantity(orderLineId, newQuantity);
    }
    @PostMapping("/confirm/{cartId}")
    public Orders confirmOrder(@RequestBody Orders orders, @PathVariable Integer cartId) {
        return iOrdersService.Confirm_Order(orders, cartId);
    }
    @GetMapping("/retrieve-all-Commandes/{Cart-id}")
    public List<Orders> getOrders(@PathVariable("Cart-id") Integer idcart) {
        return iOrdersService.Get_AllOrders(idcart);
    }
    @GetMapping("/retrieve-all-Commandes/")
    public List<Orders> getOrders() {
        return iOrdersService.Get_AllOrders();
    }
    @PutMapping("/{idOrers}/accept")
    public ResponseEntity<String> accepteOrder(@PathVariable("idOrers") Integer idOrers) {
        iOrdersService.Accept_Orders(idOrers);
        return ResponseEntity.ok("La commande a été acceptée avec succès.");
    }
    @PutMapping("/{idOrers}/refuser")
    public ResponseEntity<String> refuseOrder(@PathVariable("idOrers") Integer idOrers) {
        iOrdersService.Refuse_orders(idOrers);
        return ResponseEntity.ok("La commande a été refusée avec succès.");
    }

    @GetMapping("/generateInvoice/{orderId}")
    public void generateInvoice(@PathVariable Integer orderId, HttpServletResponse response) throws IOException {
        try {
            // Récupérer les informations de la commande à partir de la base de données
            Orders order = ordersRepositppry.findById(orderId)
                    .orElseThrow(() -> new NotFoundException("Order not found"));

            // Création du document PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            // Ajouter l'en-tête du document
            addDocumentHeader(document);

            // Ajouter les informations de la facture
            addInvoiceInfo(document, order);

            // Ajouter les articles commandés
            addOrderedItems(document, order);

            // Ajouter le résumé des totaux
            addSummary(document, order);

            // Fermer le document PDF
            document.close();

            // Envoi du document PDF en réponse
            response.setContentType("application/pdf");
            response.setContentLength(out.size());
            response.setHeader("Content-Disposition", "attachment; filename=\"invoice.pdf\"");
            OutputStream outStream = response.getOutputStream();
            outStream.write(out.toByteArray());
            outStream.flush();
            outStream.close();
        } catch (NotFoundException e) {
            // Gérer l'exception si la commande n'est pas trouvée
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            // Gérer les autres exceptions
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while generating the invoice.");
        }
    }

    private void addDocumentHeader(Document document) throws DocumentException, IOException {
        // Ajouter le logo de l'entreprise
        Image logo = Image.getInstance("src/main/java/devnatic/danceodyssey/Images/logoDance.png");
        logo.scaleToFit(90, 90);
        logo.setAbsolutePosition(document.getPageSize().getWidth() - 100, document.getPageSize().getHeight() - 80);
        document.add(logo);

        // Ajouter la signature de l'entreprise
        Image signature = Image.getInstance("src/main/java/devnatic/danceodyssey/Images/signHassen.png");
        signature.scaleToFit(100, 100);
        signature.setAbsolutePosition(document.getPageSize().getWidth() - signature.getScaledWidth() - 30, 30);
        document.add(signature);

        // Ajouter le titre de la facture
        Paragraph title = new Paragraph("Dance Odyssey", new Font(Font.TIMES_ROMAN, 22, Font.BOLD));
        title.setAlignment(Element.ALIGN_LEFT);
        title.add(Chunk.NEWLINE);
        title.add(Chunk.NEWLINE);
        title.add(new Chunk("Invoice", new Font(Font.TIMES_ROMAN, 16)));
        title.setAlignment(Element.ALIGN_CENTER);
        title.add(Chunk.NEWLINE);
        title.add(Chunk.NEWLINE);
        document.add(title);
        document.add(Chunk.NEWLINE);
    }

    private void addInvoiceInfo(Document document, Orders order) throws DocumentException {
        // Ajouter les informations de la facture
        document.add(new Paragraph("Invoice for the order N°: " + order.getOrdersId(), new Font(Font.TIMES_ROMAN, 14, Font.BOLD)));
        document.add(new Paragraph("Customer's email: " + order.getBuyer_email(), new Font(Font.TIMES_ROMAN, 14, Font.BOLD)));
        document.add(new Paragraph("Customer's address: " + order.getBuyer_address(), new Font(Font.TIMES_ROMAN, 14, Font.BOLD)));
        document.add(Chunk.NEWLINE);
    }

    private void addOrderedItems(Document document, Orders order) throws DocumentException {
        // Ajouter l'en-tête du tableau des articles commandés
        Paragraph header = new Paragraph("\n" +
                "Table of ordered items:", new Font(Font.TIMES_ROMAN, 14, Font.BOLD));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        document.add(Chunk.NEWLINE);
// Récupérer les lignes de commande associées à la commande spécifique
        List<OrderLine> orderLines = orderLineRepository.findByOrdersOrdersId(order.getOrdersId());

// Créer la table des articles commandés avec 4 colonnes
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);

// Ajouter les en-têtes de colonne
        table.addCell("Product Name");
        table.addCell("Quantity");
        table.addCell("Unit price");
        table.addCell("Total amount");

// Ajouter chaque article commandé à la table
        for (OrderLine orderLine : orderLines) {
            table.addCell( orderLine.getDescription());
            table.addCell(String.valueOf(orderLine.getNbProdO()));
            table.addCell(String.valueOf(orderLine.getProducts().getPrice()));
            table.addCell(String.valueOf(orderLine.getTotalPrice()));
        }

// Ajouter la table au document
        document.add(table);
        document.add(Chunk.NEWLINE);

    }

    private void addSummary(Document document, Orders order) throws DocumentException {
        // Ajouter le résumé des totaux

        document.add(new Paragraph("Total Tax : " + order.getTax() + "DT", new Font(Font.TIMES_ROMAN, 14, Font.BOLD)));

        document.add(new Paragraph("Total price includes Tax: " + order.getTotalPriceOders() + "DT", new Font(Font.TIMES_ROMAN, 14, Font.BOLD)));
    }

    @GetMapping("/{cartId}")
    public CART getCartById(@PathVariable Integer cartId) {
        return cartService.getCartById(cartId);
    }

    @GetMapping("/orderlines/{orderId}")
    public List<OrderLine> getOrderLinesByOrderId(@PathVariable Integer orderId) {
        return iOrdersService.getOrderLinesByOrderId(orderId);
    }
    @GetMapping("/orderlines/cart/{cartId}/productNames")
    public List<String> getProductNamesByCartId(@PathVariable Integer cartId) {
        return iOrdersService.getProductNamesByCartId(cartId);
    }
    @GetMapping("/nullOrderId")
    public List<OrderLine> getOrderLinesWithNullOrderIdByCartId() {
        return iOrderService.getOrderLinesWithNullOrderLineId();
    }
    @DeleteMapping("/orderline/{orderLineId}/cart/{cartId}")
    public OrderLine removeOrderLine(@PathVariable Integer orderLineId, @PathVariable Integer cartId) {
        return iOrderService.removeOrderLine(orderLineId, cartId);
    }
    private final CartRepository cartRepository;
    @PostMapping("addCart/{iduser}")
    public CART addCart(@PathVariable("iduser")long iduser){
        return iOrdersService.addCart(iduser);

    }    public static final String STRIPE_SECRET_KEY = "sk_test_51NS8VOAB2TRpqUHPI7CVoBJXeDxFZ7v5DQajEY1YpydKy4IGTr9ULNmoQsmW01mwfpb9ueJ2q9nX1n40f8EMH0z0000Ct4RFzp"; // Déclaration de la clé secrète Stripe comme public static final

    @PostMapping("/process-payment/{cartId}")
    public ResponseEntity<String> processPayment(@PathVariable Integer cartId, @RequestBody StripeService payload) {
        Stripe.apiKey = STRIPE_SECRET_KEY;
        try {
            // Récupérer le panier en fonction de l'ID fourni
            CART cart = cartRepository.findById(cartId).orElse(null);

            if (cart == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Cart not found with ID: " + cartId);
            }

            // Obtenir le prix total de la commande
            Float totalPrice = cart.getTotPrice();

            // Créer une charge avec les détails de paiement
            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", (int) (totalPrice * 100)); // Montant en centimes
            chargeParams.put("currency", "usd");
            chargeParams.put("source", payload.getStripeToken()); // Jeton Stripe obtenu depuis le frontend
            chargeParams.put("description", "Premium pour " + payload.getName());

            try {
                // Créer une charge en utilisant l'API Stripe
                Charge charge = Charge.create(chargeParams);

                // Paiement réussi, retourner une réponse de succès
                System.out.println("Paiement réussi.");
                return ResponseEntity.ok().build();
            } catch (StripeException e) {
                // Paiement échoué, retourner une réponse d'erreur
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erreur lors du traitement du paiement : " + e.getMessage());
            }
        } catch (Exception e) {
            // Gérer les erreurs non prévues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur s'est produite : " + e.getMessage());
        }
    }
}