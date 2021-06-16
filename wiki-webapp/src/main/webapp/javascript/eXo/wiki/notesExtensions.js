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
          getSourceLink: (activity) => activity.templateParams.page_url,
        },
      });
    },
  };
})(extensionRegistry);