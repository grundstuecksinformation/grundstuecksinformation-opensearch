package ch.so.agi.grundstuecksinformation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.a9.opensearch._11.Image;
import com.a9.opensearch._11.OpenSearchDescription;
import com.a9.opensearch._11.Url;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;


@Controller
public class MainController {
    private Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    @Value("${app.searchServiceUrl}")
    private String searchServiceUrl;
    
    @Value("${app.dataServiceUrl}")
    private String dataServiceUrl;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/ping")
    public ResponseEntity<String>  ping() {
        logger.info("opensearch endpoint");
        return new ResponseEntity<String>("opensearch endpoint", HttpStatus.OK);
    }
    
    // See grundstuecksinformation-client for opensearchdescription.xml creation.
    
    @RequestMapping(
            value = "/search/suggestions", 
            method = RequestMethod.GET, 
            //headers = "Accept=application/x-suggestions+json",
            produces = {MediaType.APPLICATION_JSON_VALUE})    
    public ResponseEntity<?> jsonSuggestions(@RequestParam(value="q", required=false) String searchTerms) throws IOException {
        logger.info("suggestions - searchTerms: " + searchTerms);
        
        if (searchTerms == null || searchTerms.trim().length() == 0) {
            return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
        }
        
        String encodedSearchText = URLEncoder.encode(searchTerms, StandardCharsets.UTF_8.toString());  
        URL url = new URL(searchServiceUrl+encodedSearchText);
        logger.info(url.toString());
        URLConnection request = url.openConnection();
        request.connect();
        
        JsonNode response = objectMapper.readTree(new InputStreamReader((InputStream) request.getContent()));        
        ArrayNode resultsArray = (ArrayNode) response.get("results");

        ArrayNode suggestions = objectMapper.createArrayNode();
        suggestions.add(searchTerms);
        
        ArrayNode completions = objectMapper.createArrayNode();
        Iterator<JsonNode> it = resultsArray.iterator();
        while(it.hasNext()) {
            JsonNode node = it.next();
            JsonNode feature = node.get("feature"); 
            String display = feature.get("display").asText();
            
            String dataproductId = feature.get("dataproduct_id").asText();
            int featureId = feature.get("feature_id").asInt();
            String idFieldName = feature.get("id_field_name").asText();
            String egrid = getEgrid(featureId, idFieldName, dataproductId);
            
            completions.add(display + " - " +egrid);
        }
        suggestions.add(completions);
               
        logger.info(suggestions.toPrettyString());
        return new ResponseEntity<JsonNode>(suggestions, HttpStatus.OK);
    }
    
    // In 'jsonSuggestions()' werden jetzt die E-GRID hinzugefügt. Somit
    // sind die Suchresultate (bei SO!GIS) eindeutig, falls eine 
    // Suggestion geklickt wird. Weiteres Prozessieren (z.B. Substringen des
    // E-GRID ist nicht notwendig).
    // FIXME:
    // Die Netzwerkroundtrips sind aber die Hölle. Testen, ob die Filter
    // die Funktion haben, damit alle E-GRID zusammen angefordert werden
    // können.
    // Auf den ersten Blick, ja: https://geo.so.ch/api/data/v1/ch.so.agi.av.grundstuecke.rechtskraeftig/?filter=[[%22t_id%22,%22=%22,207451978],%22or%22,[%22t_id%22,%22=%22,207538489]]
    // Die 'id' entspricht dem 'id_field_name'. Auch der Namen des Attributes
    // unterschiedlich ist. Im Dataservice heisst 'id' immer 'id'.
    // Hint: Wäre auch was fürs Cookbook...
    // Frage: Sollte bei der Suche nach Orten im Displaytext nicht immer alles
    // stehen, nachdem auch gesucht werden kann? Ich finde es sonst verwirrend.
    // Bei Kartenebenen dünkt es mich (!) logischer, wenn man noch "unsichtbare"
    // Metadaten durchsuchen kann.
    @GetMapping(value = "/search")
    public String searchByQuery(Model model, RedirectAttributes redirectAttributes, @RequestParam(value="q", required=false) String searchTerms) throws IOException {        
        ArrayList<SearchResult> searchResults = new ArrayList<SearchResult>();
        
        String encodedSearchText = URLEncoder.encode(searchTerms, StandardCharsets.UTF_8.toString());        
        URL url = new URL(searchServiceUrl+encodedSearchText);
        logger.info(url.toString());

        URLConnection request = url.openConnection();
        request.connect();
        
        JsonNode root = objectMapper.readTree(new InputStreamReader((InputStream) request.getContent()));        
        ArrayNode resultsArray = (ArrayNode) root.get("results");
        
        logger.info("array size {}", resultsArray.size());
       
        Iterator<JsonNode> it = resultsArray.iterator();
        while(it.hasNext()) {
            JsonNode node = it.next();
            JsonNode feature = node.get("feature");
            SearchResult result = new SearchResult();
            result.setDisplay(feature.get("display").asText());
            result.setDataproductId(feature.get("dataproduct_id").asText());
            result.setFeatureId(feature.get("feature_id").asInt());   
            result.setIdFieldName(feature.get("id_field_name").asText());
            ArrayNode bbox = (ArrayNode) feature.get("bbox");
            result.setMinX(bbox.get(0).asDouble());
            result.setMinY(bbox.get(1).asDouble());
            result.setMaxX(bbox.get(2).asDouble());
            result.setMaxY(bbox.get(3).asDouble());
            String egrid = getEgrid(result.getFeatureId(), result.getIdFieldName(), result.getDataproductId());
            result.setEgrid(egrid);
            searchResults.add(result);
            
            if (resultsArray.size() == 1) {
                redirectAttributes.addAttribute("egrid", egrid);
                return "redirect:http://grundstuecksinformation.ch";
            }
        }
     
        model.addAttribute("searchResults", searchResults);
        return "search.result.html";        
    }
    
    private String getEgrid(int featureId, String idFieldName, String dataproductId) throws IOException {
        URL url = new URL(dataServiceUrl + dataproductId + "/?filter=[[\"" + idFieldName + "\",\"=\"," + featureId + "]]");

        URLConnection request = url.openConnection();
        request.connect();
        
        JsonNode root = objectMapper.readTree(new InputStreamReader((InputStream) request.getContent()));        
        ArrayNode features = (ArrayNode) root.get("features");
        
        Iterator<JsonNode> it = features.iterator();
        JsonNode node = features.get(0);
        String egrid = node.get("properties").get("egrid").textValue();

        return egrid;
    }
}
