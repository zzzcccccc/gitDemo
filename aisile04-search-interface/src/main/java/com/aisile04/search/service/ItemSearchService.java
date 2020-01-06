package com.aisile04.search.service;

import java.util.List;
import java.util.Map;

import com.aisile04.pojo.TbItem;

public interface ItemSearchService {
	/**
	 * 搜索
	 * @param keywords
	 * @return
	 */
	public Map<String,Object> search(Map searchMap);

	public void importList(List<TbItem> list);

	public void deleteSolrByGoodsId(String text);
}
