package com.aisile.search.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aisile04.search.service.ItemSearchService;
import com.alibaba.dubbo.config.annotation.Reference;

@RestController
@RequestMapping("/itemsearch")
public class ItemSearchController {
	@Reference
	ItemSearchService itemSearchService;
	
	@RequestMapping("/search")
	public Map<String, Object> search(@RequestBody Map searchMap ){
		return  itemSearchService.search(searchMap);
	}
	
	
}
