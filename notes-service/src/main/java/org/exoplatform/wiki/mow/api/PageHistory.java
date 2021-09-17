package org.exoplatform.wiki.mow.api;

import lombok.Data;

import java.util.Date;

@Data
public class PageHistory {

	private Long versionNumber;

	private String author;

	private String authorFullName;

	private Date createdDate;

	private Date updatedDate;

	private String content;
}
