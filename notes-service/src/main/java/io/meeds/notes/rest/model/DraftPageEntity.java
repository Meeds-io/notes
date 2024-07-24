package io.meeds.notes.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftPageEntity extends PageEntity implements Serializable {

  private String  targetPageId;

  private boolean newPage;

}
