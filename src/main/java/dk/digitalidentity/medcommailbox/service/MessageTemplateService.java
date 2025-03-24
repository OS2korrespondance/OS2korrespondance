package dk.digitalidentity.medcommailbox.service;

import dk.digitalidentity.medcommailbox.dao.MessageTemplateDao;
import dk.digitalidentity.medcommailbox.dao.model.MessageTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MessageTemplateService {

	@Autowired
	private MessageTemplateDao messageTemplateDao;

	public void save (MessageTemplate messageTemplate) {
		messageTemplateDao.save(messageTemplate);
	}

	public Optional<MessageTemplate> get(long id) {
		return messageTemplateDao.findById(id);
	}

	public List<MessageTemplate> getAllSortedByName () {
		return messageTemplateDao.findAll(Sort.by(Sort.Order.desc("templateName")));
	}
}
