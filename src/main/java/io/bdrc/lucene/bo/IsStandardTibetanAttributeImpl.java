package io.bdrc.lucene.bo;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

public class IsStandardTibetanAttributeImpl extends AttributeImpl implements IsStandardTibetanAttribute {
    
    private boolean isStandardTibetan = true;

    @Override
    public void setIsStandardTibetan(boolean isStandardTibetan) {
        this.isStandardTibetan = isStandardTibetan;
    }

    @Override
    public boolean getIsStandardTibetan() {
        return this.isStandardTibetan;
    }
    
    @Override
    public void clear() {
        this.isStandardTibetan = true;
    }

    @Override
    public void copyTo(AttributeImpl target) {
        ((IsStandardTibetanAttribute) target).setIsStandardTibetan(this.isStandardTibetan);
    }

    @Override
    public void reflectWith(AttributeReflector reflector) {
        reflector.reflect(IsStandardTibetanAttribute.class, "isStandardTibetan", this.isStandardTibetan);
    }   

}
