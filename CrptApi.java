/*
почитать что такое подпись в програмировании (signature)
почитать как устроены http запросы

создать Http клиент
1 делать http запрос
2  thread-safe
3 ограничение на количество запросов к API за интервал времени
4 Реализация должна быть максимально удобной для последующего расширения функционала
5 Все дополнительные классы, которые используются должны быть внутренними


 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private int requestLimit;
    public final AtomicInteger requestCounter = new AtomicInteger(0);

    public final List<Document> documentsToSend = Collections.synchronizedList(new ArrayList<Document>());
    public final AtomicBoolean started = new AtomicBoolean(false);


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();


        if (started.compareAndSet(false, true)) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> this.limiterCallback(), 1, 1, timeUnit);
            this.requestLimit = requestLimit;
        }
    }


    private void limiterCallback() {
        this.requestCounter.set(0);
        int amountToSend = Math.min(this.documentsToSend.size(), this.requestLimit);

        for (int i = 0; i < amountToSend; i++) {
            try {

                this.sendDocument(this.documentsToSend.remove(0));

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public String sendDocument(Document document) throws IOException, InterruptedException {
        if (this.requestCounter.get() >= this.requestLimit) {
            synchronized (documentsToSend) {
                documentsToSend.add(document);
            }

            return null;
        } else {
            this.requestCounter.incrementAndGet();
            String baseUrl = "https://ismp.crpt.ru";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v3/lk/documents/create"))
                    .setHeader("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(document)))
                    .build();

            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        }

    }

    public static class Document {
        @JsonProperty("description")
        private DocumentDescription description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private String productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("products")
        private DocumentProduct[] products;

        @JsonProperty
        private String reg_date;
        @JsonProperty
        private String reg_number;


        public Document(String docId, DocumentDescription description, String docStatus, String docType,
                        boolean importRequest, String ownerInn, String participantInn, String producerInn,
                        String productionDate, String productionType, DocumentProduct[] products, String reg_date, String reg_number) {
            this.docId = docId;
            this.description = description;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }


    }

    public static class DocumentDescription {
        @JsonProperty("participantInn")
        private final String participantInn;

        public DocumentDescription(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    public static class DocumentProduct {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonProperty("certificate_document_date")
        private String certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        private String productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;

        public DocumentProduct(String certificateDocument, String certificateDocumentDate,
                               String certificateDocumentNumber, String ownerInn, String producerInn,
                               String productionDate, String tnvedCode, String uitCode, String uituCode) {
            this.certificateDocument = certificateDocument;
            this.certificateDocumentDate = certificateDocumentDate;
            this.certificateDocumentNumber = certificateDocumentNumber;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.tnvedCode = tnvedCode;
            this.uitCode = uitCode;
            this.uituCode = uituCode;
        }

    }
}


