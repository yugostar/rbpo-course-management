package ru.mtuci.coursemanagement.controller;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.xml.sax.InputSource;

import java.io.StringReader;

@RestController
public class XmlController {

    @PostMapping(
            value = "/api/xml/parse",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    public String parse(@RequestBody String xml) throws Exception {

        SAXReader reader = new SAXReader();

        // A05: prevent XXE / entity expansion
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // extra safety: block any external entity resolution
        reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

        Document doc = reader.read(new StringReader(xml));
        return doc.getRootElement().getText();
    }
}
