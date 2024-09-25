package org.exoplatform.wiki.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "WikiEmotionIconEntity")
@Table(name = "WIKI_EMOTION_ICONS")
@NamedQueries({
        @NamedQuery(name = "emotionIcon.getEmotionIconByName", query = "SELECT e FROM WikiEmotionIconEntity e WHERE e.name = :name")
})
public class EmotionIconEntity {
  @Id
  @SequenceGenerator(name="SEQ_WIKI_EMOTION_ICONS_ICON_ID", sequenceName="SEQ_WIKI_EMOTION_ICONS_ICON_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_EMOTION_ICONS_ICON_ID")
  @Column(name = "EMOTION_ICON_ID")
  private long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "IMAGE", length = 20971520)
  private byte[] image;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}
