package com.aisile04.search.service.impl;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aisile04.search.service.ItemSearchService;
@Component
public class ItemSearchDeleteListener implements MessageListener {

	@Autowired
	private ItemSearchService itemSearchService;
	
	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		TextMessage textMessage = (TextMessage)message;
		try {
			itemSearchService.deleteSolrByGoodsId(textMessage.getText());
			System.out.println("记录日志删除成功");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("记录日志删除失败");
		}
	}

}
