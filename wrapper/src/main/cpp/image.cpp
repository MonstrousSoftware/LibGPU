
#include "image.h"

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"



#ifdef __cplusplus
extern "C" {
#endif

EXPORT gdx2d_pixmap* gdx2d_load(const unsigned char *buffer, uint32_t len) {
	int32_t width, height, format;

	//printf("gdx2d_load: len = %d\n", len);

	const unsigned char* pixels = stbi_load_from_memory(buffer, len, &width, &height, &format, 4);  // force 4 components: RGBA
	if (pixels == NULL){
	    //printf("gdx2d_load: load error\n");
		return NULL;
	}


	gdx2d_pixmap* pixmap = (gdx2d_pixmap*)malloc(sizeof(gdx2d_pixmap));
	if (!pixmap) return 0;
	pixmap->width = (uint32_t)width;
	pixmap->height = (uint32_t)height;
	pixmap->format = (uint32_t)format;          // todo convert
	pixmap->blend = GDX2D_BLEND_SRC_OVER;
	pixmap->scale = GDX2D_SCALE_BILINEAR;
	pixmap->pixels = pixels;
	//printf("gdx2d_load: w = %d, h = %d, format = %d\n", pixmap->width, pixmap->height, pixmap->format);
	//printf("pixmap at %p\n", pixmap);
	return pixmap;
}

EXPORT void gdx2d_free(const gdx2d_pixmap* pixmap) {
	free((void*)pixmap->pixels);
	free((void*)pixmap);
}


#ifdef __cplusplus
}
#endif


