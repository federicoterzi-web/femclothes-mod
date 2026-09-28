## Model creation:

Before getting started, the json file names need to be all lower case and contain no special characters like é or spaces.

Second there are 2 render_types, the one for bedrock models is called bedrock. The other is called simple which is kind of a legacy render type used by my mod, its used for example to create the different hair parts, basically makes a 2d texture into a 3d model.

A few things to take into account when creating a blockbench model:
1. You cannot use decimals in the sizes of cubes. If you want to make your cube bigger or smaller use the 4th value that one can be in decimals and can be negative
2. Currently only box-uv is supported, no per face uv
3. Currently blockbench animations are not supported. I have my own custom animation code

If you are having issues, please use an online json validator like https://jsonformatter.curiousconcept.com/ first and look at the .minecraft/logs/moreplayermodels.log

For a quick if guide watch https://www.youtube.com/watch?v=wXK-uBOnTFQ

Tip for creating your own parts, put them in .minecraft/moreplayermodels/assets/moreplayermodels/parts folder, if you make changes to your file, hit f3 + t to reload it and see the changes

## Json attributes:
Attributes with a * are required

- *name
- *menu
- *author
- parentId
- enabled
- texture
- *body_part					NONE, HEAD, BODY, LEGS, ARMS, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
- hidden_parts				[NONE, HEAD, BODY, LEGS, ARMS, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG]
---
- *preview_rotation
- translate
- scale
- rotate_offset
- rotate
---
- default_use_player_skins
- disable_custom_textures
---
- *render_type					BEDROCK, SIMPLE
- *render_data
---
- animation_type				NONE, BEARD, HAIR, WINGS, WINGS2, LEGS, ARMS
- animation_inherit
- animation_data
  - animation_length
  - animation_speed
  - loop
  - additional
  - bones						{ animation : { translate: [], rotation: []}}


- animations 	NONE, SIT, SLEEP, HUG, CROUCH, DANCE, AIM, CRAWL, POINT, CRY, WAVE, BOW, NO, YES, DEATH, WALK, IDLE, FLY, FLY_IDLE, STATIC, SWIM, WAG