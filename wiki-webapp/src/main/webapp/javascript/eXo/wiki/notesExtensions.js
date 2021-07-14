(function(extensionRegistry) {
  return {
    init: () => {
      extensionRegistry.registerExtension('activity', 'type', {
        type: 'ks-wiki:spaces',
        options: {
          canEdit: () => false,
          supportsThumbnail: true,
          useSameViewForMobile: true,
          thumbnailProperties: {
            height: '90px',
            width: '90px',
            noBorder: true,
          },
          getThumbnail: () => '/wiki/images/wiki.png',
          getTitle: activity => activity && activity.title || '',
          getSummary: (activity) => activity.templateParams
                                    && Vue.prototype.$utils.htmlToText(activity.templateParams.page_exceprt),
          getSourceLink: (activity) => {
            try {
              const templateParams = activity.templateParams || {};
              if (templateParams.page_url) {
                return activity.templateParams.page_url;
              }
              if (templateParams.page_type === 'group') {
                return `${eXo.env.portal.context}/g/${templateParams.page_owner.replace(/\//g, ':')}/${templateParams.page_owner.replace('/spaces/', ':')}/wiki/${templateParams.page_id}`;
              } else if(templateParams.page_type === 'portal') {
                return `${eXo.env.portal.context}/${templateParams.page_owner}/wiki/${templateParams.page_id}`;
              }
            } catch (e) {
              // eslint-disable-next-line no-console
              console.warn('It seems that notes URL building is compromised. The activity will be displayed anyway with empty link', e);
            }
            return '#';
          },
          canShare: () => true,
        },
      });
    },
  };
})(extensionRegistry);