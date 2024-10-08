package org.khtml.enjoyallback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.khtml.enjoyallback.dto.WelfareInfoDto;
import org.khtml.enjoyallback.entity.CrawledData;
import org.khtml.enjoyallback.repository.CrawledDataRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class DynamicWebCrawlerService {
    private final RestTemplate restTemplate;
    private final static int MAX = 1000;
    private static final Logger logger = LoggerFactory.getLogger(WebCrawlerService.class);
    final private CrawledDataRepository crawledDataRepository;

//    @PostConstruct
    @Scheduled(initialDelay = 10_000, fixedDelay = Long.MAX_VALUE)
    public void init() throws Exception {
        for (int i = 1; i < MAX; ++i) {
            sendPostRequest(i);
        }
    }
    /*
    https://www.bokjiro.go.kr/ssis-tbu/TWAT52005M/twataa/wlfareInfo/selectWlfareInfo.do
    {"dmSearchParam":{"page":"5","onlineYn":"","searchTerm":"","tabId":"1","orderBy":"date","bkjrLftmCycCd":"","daesang":"","period":"","age":"","region":"경기도 용인시","jjim":"","subject":"","favoriteKeyword":"Y","sido":"","gungu":"","endYn":"N"},"dmScr":{"curScrId":"tbu/app/twat/twata/twataa/TWAT52005M","befScrId":""}}
     */
    @Async
    public void sendPostRequest(int page) throws Exception {
        logger.info(page + " / " + MAX);
        String url = "https://www.bokjiro.go.kr/ssis-tbu/TWAT52005M/twataa/wlfareInfo/selectWlfareInfo.do";

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 바디 설정
        Map<String, Object> dmSearchParam = new HashMap<>();
        dmSearchParam.put("page", page);
        dmSearchParam.put("onlineYn", "");
        dmSearchParam.put("searchTerm", "");
        dmSearchParam.put("tabId", "1"); // 중앙 부처
        dmSearchParam.put("orderBy", "date");
        dmSearchParam.put("bkjrLftmCycCd", "");
        dmSearchParam.put("daesang", "");
        dmSearchParam.put("period", "노년");
        dmSearchParam.put("age", "");
        dmSearchParam.put("region", "경기도 용인시");
        dmSearchParam.put("jjim", "");
        dmSearchParam.put("subject", "");
        dmSearchParam.put("favoriteKeyword", "Y");
        dmSearchParam.put("sido", "");
        dmSearchParam.put("gungu", "");
        dmSearchParam.put("endYn", "N");

        Map<String, Object> dmScr = new HashMap<>();
        dmScr.put("curScrId", "tbu/app/twat/twata/twataa/TWAT52005M");
        dmScr.put("befScrId", "");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("dmSearchParam", dmSearchParam);
        requestBody.put("dmScr", dmScr);

        // 요청 엔티티 생성
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // POST 요청 보내기
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        List<WelfareInfoDto> welfareInfos = parseWelfareInfoList(response);
        for (WelfareInfoDto welfareInfo : welfareInfos) {
            String id = welfareInfo.getWelfareInfoId();
            url = "https://www.bokjiro.go.kr/ssis-tbu/twataa/wlfareInfo/moveTWAT52011M.do?wlfareInfoId="+id+"&wlfareInfoReldBztpCd=01";
            logger.info("Connect : " + url);
            String htmlDOM = scrapeDynamicPage(url);
            CrawledData crawledData = new CrawledData();
            crawledData.setUrl(url);
            crawledData.setContent(htmlDOM);
            crawledData.setTitle(welfareInfo.getWelfareInfoName());
            logger.info("SAVE!!!!!!");
            crawledDataRepository.save(crawledData);
        }
    }
    public List<WelfareInfoDto> parseWelfareInfoList(ResponseEntity<String> responseEntity) {
        List<WelfareInfoDto> welfareInfoList = new ArrayList<>();

        try {
            String jsonResponse = responseEntity.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            JsonNode dsServiceList1Node = rootNode.path("dsServiceList1");

            if (dsServiceList1Node.isArray()) {
                for (JsonNode node : dsServiceList1Node) {
                    WelfareInfoDto welfareInfo = objectMapper.treeToValue(node, WelfareInfoDto.class);
                    welfareInfoList.add(welfareInfo);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return welfareInfoList;

    }
    public String scrapeDynamicPage(String url) throws Exception {
        System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\chromedriver-win64\\chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
//        WebDriver driver = new ChromeDriver(options); // dev
        WebDriver driver = new RemoteWebDriver(new URL("http://selenium-chrome:4444/wd/hub"), options); // deploy

        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
//            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("pageLoadingImage")));

//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("pageLoadingImage")));
            // #uuid-73 식별자를 가진 <div> 요소 찾기
            WebElement divElement = driver.findElement(By.id("uuid-73"));
            return divElement.getAttribute("outerHTML");
        } finally {
            driver.quit();
        }
    }
}
