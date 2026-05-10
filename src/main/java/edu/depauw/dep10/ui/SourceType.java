package edu.depauw.dep10.ui;

public interface SourceType {

    SourceType Pep10UserFull = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 User Code";
        }
    };
    
    SourceType Pep10UserBare = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 Bare Metal";
        }
    };
    
    SourceType Pep10System = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 System Code";
        }
    };
    
    SourceType DeCLan = new SourceType() {
        @Override
        public String toString() {
            return "DeCLan";
        }
    };
}
