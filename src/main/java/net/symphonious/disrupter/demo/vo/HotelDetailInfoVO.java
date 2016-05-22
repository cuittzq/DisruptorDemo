package net.symphonious.disrupter.demo.vo;

import java.math.BigDecimal;

public class HotelDetailInfoVO {
	private BigDecimal lowestPrice;
	private Integer resourceId;

	public HotelDetailInfoVO(BigDecimal lowestPrice, Integer resourceId) {
		this.lowestPrice = lowestPrice;
		this.resourceId = resourceId;
	}

	public BigDecimal getLowestPrice() {
		return lowestPrice;
	}

	public void setLowestPrice(BigDecimal lowestPrice) {
		this.lowestPrice = lowestPrice;
	}

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

}
