define [
  'navigator/filters/string-filters'
], (
  StringFilterView
) ->

  class DateFilterView extends StringFilterView

    render: ->
      super
      @detailsView.$('input').prop 'placeholder', '1970-01-31'
