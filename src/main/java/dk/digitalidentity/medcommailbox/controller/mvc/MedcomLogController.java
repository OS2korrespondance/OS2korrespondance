package dk.digitalidentity.medcommailbox.controller.mvc;

import java.io.StringWriter;

import dk.digitalidentity.medcommailbox.security.RequireUserAccess;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.medcommailbox.dao.model.MedcomLog;
import dk.digitalidentity.medcommailbox.service.MedcomLogService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequireUserAccess
public class MedcomLogController {
	
	@Autowired
	private MedcomLogService logService;
	
	@GetMapping("/logs")
	public String getLogs(Model model) {
		model.addAttribute("logsIncomming", logService.getByIncomming(true));
		model.addAttribute("logsOutgoing", logService.getByIncomming(false));
		return "medcom_logs";
	}

	@GetMapping("/logs/{id}")
	public String getLog(Model model, @PathVariable long id) {
		MedcomLog log = logService.getById(id);
		if (log == null) {
			return "redirect:/logs";
		}

		if (log.getMailXml() != null) {
			log.setMailXml(prettyPrintXml(log.getMailXml()));
		}

		if (log.getReceiptXml() != null) {
			log.setReceiptXml(prettyPrintXml(log.getReceiptXml()));
		}

		if (log.getNegativeReceiptReason() != null) {
			log.setNegativeReceiptReason(log.getNegativeReceiptReason().replace("\n", "<br/>"));
		}
		
		model.addAttribute("log", log);
		return "view_log";		
	}
	
	private String prettyPrintXml(String xmlString) {
	    try {
	        OutputFormat format = OutputFormat.createPrettyPrint();
	        format.setIndentSize(4);
	        format.setSuppressDeclaration(false);
	        format.setEncoding("UTF-8");

	        org.dom4j.Document document = DocumentHelper.parseText(xmlString);
	        StringWriter sw = new StringWriter();
	        XMLWriter writer = new XMLWriter(sw, format);
	        writer.write(document);
	        return sw.toString();
	    } catch (Exception e) {
	    	log.error("Failed to pretty print xml token: " + xmlString);
	        return xmlString;
	    }
	}
}
